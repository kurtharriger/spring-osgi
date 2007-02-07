/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.osgi.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.ConstantException;
import org.springframework.core.Constants;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.support.LocalBundleContext;
import org.springframework.osgi.service.collection.OsgiServiceList;
import org.springframework.osgi.service.support.ClassTargetSource;
import org.springframework.osgi.service.support.RetryTemplate;
import org.springframework.osgi.service.support.cardinality.OsgiServiceDynamicInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for OSGi services. Returns a dynamic proxy which handles the
 * lookup, retrieval and can cope with the dynamic nature of the OSGi platform.
 * 
 * @author Costin Leau
 * @author Adrian Colyer
 * @author Hal Hildebrand
 * 
 */
public class OsgiServiceProxyFactoryBean implements FactoryBean, InitializingBean, DisposableBean, BundleContextAware {

	public static final String FILTER_ATTRIBUTE = "filter";

	public static final String INTERFACE_ATTRIBUTE = "interface";

	public static final String CARDINALITY_ATTRIBUTE = "cardinality";

	/**
	 * Reference classloading options costants.
	 * 
	 * @author Costin Leau
	 */
	public abstract static class ReferenceClassLoadingOptions {
		public static final int CLIENT = 0;

		public static final int SERVICE_PROVIDER = 1;

		public static final int UNMANAGED = 2;
	}

	/**
	 * Cardinality constants.
	 * 
	 * @author Costin Leau
	 */
	public abstract static class Cardinality {
		public static final int C_0__1 = 0;

		public static final int C_0__N = 1;

		public static final int C_1__1 = 2;

		public static final int C_1__N = 3;
	}

	private static final Log logger = LogFactory.getLog(OsgiServiceProxyFactoryBean.class);

	private static final Constants CARDINALITY = new Constants(Cardinality.class);

	private static final Constants REFERENCE_CL_OPTIONS = new Constants(ReferenceClassLoadingOptions.class);

	public static final String OBJECTCLASS = "objectClass";

	private BundleContext bundleContext;

	private RetryTemplate retryTemplate = new RetryTemplate();

	private int cardinality = Cardinality.C_1__1;

	private int contextClassloader = ReferenceClassLoadingOptions.CLIENT;

	// not required to be an interface, but usually should be...
	private Class serviceType;

	// filter used to narrow service matches, may be null
	private String filter;

	// Constructed object of this factory
	private Object proxy;

	// Cumulated filter string between the specified classes/interfaces and the
	// given filter
	private String filterStringForServiceLookup;

	// service lifecycle listener
	private TargetSourceLifecycleListener[] listeners = new TargetSourceLifecycleListener[0];

	private String serviceBeanName;

	public static int translateCardinality(String cardinality) {
		return CARDINALITY.asNumber("C_".concat(cardinality.replace('.', '_'))).intValue();
	}

	public static boolean atMostOneExpected(int c) {
		return Cardinality.C_0__1 == c || Cardinality.C_1__1 == c;
	}

	public static boolean atLeastOneRequired(int c) {
		return Cardinality.C_1__1 == c || Cardinality.C_1__N == c;
	}

	public static boolean moreThanOneExpected(int c) {
		return Cardinality.C_0__N == c || Cardinality.C_1__N == c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public Object getObject() throws Exception {
		return proxy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class getObjectType() {
		// TODO: return a class that returns all required interfaces
		if (proxy != null)
			return proxy.getClass();

		// TODO: clarify the Collection/List/Set contract
		if (moreThanOneExpected(cardinality))
			return List.class;

		return getInterface();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.bundleContext, "Required bundleContext property was not set");
		Assert.notNull(getInterface(), "Required serviceType property was not set");

		// validate and create the filter
		createFilter();

		if (atMostOneExpected(cardinality))
			proxy = createSingleServiceProxy();
		else
			proxy = createMultiServiceCollection(getInterface(), filterStringForServiceLookup);
	}

	protected Object createSingleServiceProxy() throws Exception {

		ProxyFactory factory = new ProxyFactory();

		// mold the proxy
		configureFactoryForClass(factory, getInterface());

		// TODO: the same advices should be available for the multi case/service
		// collection
		// add advices
		//

		// TCCL support
		// addContextClassLoaderSupport(factory, reference);

		// Bundle Ctx
		addLocalBundleContextSupport(factory);

		// dynamic retry interceptor
		addOsgiRetryInterceptor(factory, getInterface(), filterStringForServiceLookup, listeners);

		// TODO: should these be enabled ?
		// factory.setFrozen(true);
		// factory.setOptimize(true);
		// factory.setOpaque(true);

		return factory.getProxy(ProxyFactory.class.getClassLoader());
	}

	protected Object createMultiServiceCollection(Class clazz, String filter) {
		return new OsgiServiceList(clazz.getName(), filter, bundleContext, contextClassloader);
	}

	protected void addOsgiRetryInterceptor(ProxyFactory factory, Class clazz, String filter,
			TargetSourceLifecycleListener[] listeners) {
		OsgiServiceDynamicInterceptor lookupAdvice = new OsgiServiceDynamicInterceptor(bundleContext,
				contextClassloader);
		lookupAdvice.setListeners(listeners);
		lookupAdvice.setClass(clazz.getName());
		lookupAdvice.setFilter(filter);
		lookupAdvice.setRetryTemplate(new RetryTemplate(retryTemplate));

		lookupAdvice.afterPropertiesSet();

		factory.addAdvice(lookupAdvice);
	}

	/**
	 * Based on the given class, use JDK Proxy or CGLIB instrumentation when
	 * generating the proxy.
	 * 
	 * @param factory
	 * @param clazz
	 */
	protected void configureFactoryForClass(ProxyFactory factory, Class clazz) {
		if (clazz.isInterface()) {
			factory.setInterfaces(new Class[] { clazz });
		}
		else {
			factory.setTargetSource(new ClassTargetSource(clazz));
			factory.setProxyTargetClass(true);
		}
	}

	protected void createFilter() {
		// if (getFilter() != null)
		{
			// this call forces parsing of the filter string to generate an
			// exception right
			// now if it is not well-formed
			try {
				FrameworkUtil.createFilter(getFilterStringForServiceLookup());
			}
			catch (InvalidSyntaxException ex) {
				throw (IllegalArgumentException) new IllegalArgumentException("Filter string '" + getFilter()
						+ "' set on OsgiServiceProxyFactoryBean has invalid syntax: " + ex.getMessage()).initCause(ex);
			}
		}
	}

	/**
	 * Add the local bundle context support.
	 * 
	 * @param factory
	 */
	protected void addLocalBundleContextSupport(ProxyFactory factory) {
		// TODO: make this customizable
		// Add advice for pushing the bundle context
		factory.addAdvice(new LocalBundleContext(bundleContext.getBundle()));
	}

	/**
	 * @return Returns the serviceType.
	 */
	public Class getInterface() {
		return this.serviceType;
	}

	/**
	 * The type that the OSGi service was registered with
	 */
	public void setInterface(Class serviceType) {
		this.serviceType = serviceType;
	}

	/**
	 * The optional cardinality attribute allows a reference cardinality to be
	 * specified (0..1, 1..1, 0..n, or 1..n). The default is '1..1'.
	 * 
	 * @param cardinality
	 */
	public void setCardinality(String cardinality) {
		// transform string to the constant representation
		// a. C_ is appended to the string
		// b. . -> _

		if (cardinality != null) {
			try {
				this.cardinality = translateCardinality(cardinality);
				return;
			}
			catch (ConstantException ex) {
				// catch exception to not confuse the user with a different
				// constant name(will be handled afterwards)
			}
		}
		throw new IllegalArgumentException("invalid constant, " + cardinality);
	}

	public void setContextClassloader(String classLoaderManagementOption) {
		// transform "-" into "_" (for service-provider)
		Assert.notNull(classLoaderManagementOption, "non-null argument required");
		String option = classLoaderManagementOption.replace('-', '_');
		this.contextClassloader = REFERENCE_CL_OPTIONS.asNumber(option).intValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.context.BundleContextAware#setBundleContext(org.osgi.framework.BundleContext)
	 */
	public void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}

	/**
	 * How many times should we attempt to rebind to a target service if the
	 * service we are currently using is unregistered. Default is 3 times. <p/>
	 * Changing this property after initialization is complete has no effect.
	 * 
	 * @param maxRetries The maxRetries to set.
	 */
	public void setRetryTimes(int maxRetries) {
		this.retryTemplate.setRetryNumbers(maxRetries);
	}

	public int getRetryTimes() {
		return this.retryTemplate.getRetryNumbers();
	}

	/**
	 * How long should we wait between failed attempts at rebinding to a service
	 * that has been unregistered. <p/>
	 * 
	 * @param millisBetweenRetries The millisBetweenRetries to set.
	 */
	public void setTimeout(long millisBetweenRetries) {
		this.retryTemplate.setWaitTime(millisBetweenRetries);
	}

	public long getTimeout() {
		return this.retryTemplate.getWaitTime();
	}

	// this is as nasty as dynamic sql generation.
	// build an osgi filter string to find the service we are
	// looking for.
	private String getFilterStringForServiceLookup() {
		if (filterStringForServiceLookup != null) {
			return filterStringForServiceLookup;
		}
		StringBuffer sb = new StringBuffer();
		boolean andFilterWithInterfaceName = (getFilter() != null) || (getServiceBeanName() != null);
		if (andFilterWithInterfaceName) {
			sb.append("(&");
		}
		if (getFilter() != null) {
			sb.append(getFilter());
		}
		sb.append("(");
		sb.append(OBJECTCLASS);
		sb.append("=");
		sb.append(getInterface().getName());
		sb.append(")");

		if (getServiceBeanName() != null) {
			sb.append("(");
			sb.append(BeanNameServicePropertiesResolver.BEAN_NAME_PROPERTY_KEY);
			sb.append("=");
			sb.append(getServiceBeanName());
			sb.append(")");
		}

		if (andFilterWithInterfaceName) {
			sb.append(")");
		}

		String completeFilter = sb.toString();
		if (StringUtils.hasText(completeFilter)) {
			filterStringForServiceLookup = completeFilter;
			return filterStringForServiceLookup;
		}
		else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		// FIXME: add destroy behavior
	}

	/**
	 * @return Returns the filter.
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * @param filter The filter to set.
	 */
	public void setFilter(String filter) {
		this.filter = filter;
	}

	/**
	 * @return Returns the listeners.
	 */
	public TargetSourceLifecycleListener[] getListeners() {
		return listeners;
	}

	/**
	 * @param listeners The listeners to set.
	 */
	public void setListeners(TargetSourceLifecycleListener[] listeners) {
		this.listeners = listeners;
	}

	/**
	 * @return Returns the serviceBeanName.
	 */
	public String getServiceBeanName() {
		return serviceBeanName;
	}

	/**
	 * To find a bean published as a service by the OsgiServiceExporter, simply
	 * set this property. You may specify additional filtering criteria if
	 * needed (using the filter property) but this is not required.
	 * 
	 * @param serviceBeanName The serviceBeanName to set.
	 */
	public void setServiceBeanName(String serviceBeanName) {
		this.serviceBeanName = serviceBeanName;
	}

	// FIXME: this should be removed - bean-name-> service-bean-name
	public void setBeanName(String beanName) {
		setServiceBeanName(beanName);
	}

}
