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
package org.springframework.osgi.service.importer;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.support.LocalBundleContext;
import org.springframework.osgi.service.BeanNameServicePropertiesResolver;
import org.springframework.osgi.service.CardinalityOptions;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.osgi.service.collection.OsgiServiceCollection;
import org.springframework.osgi.service.collection.OsgiServiceList;
import org.springframework.osgi.service.interceptor.OsgiServiceDynamicInterceptor;
import org.springframework.osgi.service.support.RetryTemplate;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

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

	private static final Log log = LogFactory.getLog(OsgiServiceProxyFactoryBean.class);

	public static final String FILTER_ATTRIBUTE = "filter";

	public static final String INTERFACE_ATTRIBUTE = "interface";

	public static final String CARDINALITY_ATTRIBUTE = "cardinality";

	public static final String OBJECTCLASS = "objectClass";

	private BundleContext bundleContext;

	private RetryTemplate retryTemplate = new RetryTemplate();

	private int cardinality = CardinalityOptions.C_1__1;

	private int contextClassloader = ReferenceClassLoadingOptions.CLIENT;

	// not required to be an interface, but usually should be...
	private Class[] serviceTypes;

	// filter used to narrow service matches, may be null
	private String filter;

	// Constructed object of this factory
	private Object proxy;

	// Cumulated filter string between the specified classes/interfaces and the
	// given filter
	private Filter unifiedFilter;

	// service lifecycle listener
	private TargetSourceLifecycleListener[] listeners = new TargetSourceLifecycleListener[0];

	private String serviceBeanName;

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
		if (this.proxy != null) {
			return this.proxy.getClass();
		}

		// TODO: clarify the Collection/List/Set contract
		if (CardinalityOptions.moreThanOneExpected(cardinality))
			return List.class;

		if (serviceTypes != null && serviceTypes.length == 1) {
			return serviceTypes[0];
		}

		// normally this is returned, only if multiple interfaces are specified
		// but
		// the proxy hasn't been created yet.

		return null;
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
		Assert.notNull(serviceTypes, "Required serviceTypes property was not set");

		// clean up parent classes
		serviceTypes = OsgiServiceUtils.removeParents(serviceTypes);
		// validate specified classes
		Assert.isTrue(doesContainMultipleConcreteClasses(serviceTypes),
			"more then one concrete class specified; cannot create proxy");

		String filterWithClasses = OsgiFilterUtils.unifyFilter(serviceTypes, filter);

		if (log.isTraceEnabled())
			log.trace("unified classes=" + ObjectUtils.nullSafeToString(serviceTypes) + " and filter=[" + filter
					+ "]  in=[" + filterWithClasses + "]");

		// add the serviceBeanName constraint
		String filterWithServiceBeanName = OsgiFilterUtils.unifyFilter(
			BeanNameServicePropertiesResolver.BEAN_NAME_PROPERTY_KEY.toString(), new String[] { serviceBeanName },
			filterWithClasses);

		if (log.isTraceEnabled())
			log.trace("added serviceBeanName " + ObjectUtils.nullSafeToString(serviceBeanName) + " and filter=["
					+ filterWithClasses + "]  in=[" + filterWithServiceBeanName + "]");

		// create (which implies validation) the actual filter
		unifiedFilter = OsgiFilterUtils.createFilter(filterWithServiceBeanName);

		if (CardinalityOptions.atMostOneExpected(cardinality))
			proxy = createSingleServiceProxy();
		else
			proxy = createMultiServiceCollection(unifiedFilter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		// FIXME: add destroy behavior
	}

	protected boolean doesContainMultipleConcreteClasses(Class[] classes) {
		boolean concreteClassFound = false;
		// check if is more then one class specified
		for (int i = 0; i < serviceTypes.length; i++) {
			if (!serviceTypes[i].isInterface()) {
				if (concreteClassFound)
					return false;
				else
					concreteClassFound = true;
			}
		}
		return true;
	}

	protected Object createSingleServiceProxy() throws Exception {
		if (log.isDebugEnabled())
			log.debug("creating a singleService proxy");

		ProxyFactory factory = new ProxyFactory();

		// mold the proxy
		configureFactoryForClass(factory, serviceTypes);

		// TODO: the same advices should be available for the multi case/service
		// collection

		// Bundle Ctx
		addLocalBundleContextSupport(factory);

		// dynamic retry interceptor / context classloader
		addOsgiRetryInterceptor(factory, unifiedFilter, listeners);

		// TODO: should these be enabled ?
		// factory.setFrozen(true);
		// factory.setOptimize(true);
		// factory.setOpaque(true);

		return factory.getProxy(ProxyFactory.class.getClassLoader());
	}

	protected Object createMultiServiceCollection(Filter filter) {
		if (log.isDebugEnabled())
			log.debug("creating a multi-value/collection proxy");

		OsgiServiceCollection collection = new OsgiServiceList(filter, bundleContext);

		collection.setListeners(listeners);
		collection.setContextClassLoader(contextClassloader);
		// setup done
		collection.afterPropertiesSet();
		return collection;
	}

	protected void addOsgiRetryInterceptor(ProxyFactory factory, Filter filter,
			TargetSourceLifecycleListener[] listeners) {
		OsgiServiceDynamicInterceptor lookupAdvice = new OsgiServiceDynamicInterceptor(bundleContext,
				contextClassloader);
		lookupAdvice.setListeners(listeners);
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
	 * @param classes
	 */
	protected void configureFactoryForClass(ProxyFactory factory, Class[] classes) {
		for (int i = 0; i < classes.length; i++) {
			Class clazz = classes[i];

			if (clazz.isInterface()) {
				factory.addInterface(clazz);
			}
			else {
				factory.setTargetClass(clazz);
				factory.setProxyTargetClass(true);
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
	public Class[] getInterface() {
		return this.serviceTypes;
	}

	/**
	 * The type that the OSGi service was registered with
	 */
	// TODO: normally the noum should be plural, not singular (which means
	// changing the spring-osgi.xsd a bit)
	public void setInterface(Class[] serviceType) {
		this.serviceTypes = serviceType;
	}

	/**
	 * The optional cardinality attribute allows a reference cardinality to be
	 * specified (0..1, 1..1, 0..n, or 1..n). The default is '1..1'.
	 * 
	 * @param cardinality
	 */
	public void setCardinality(String cardinality) {
		this.cardinality = CardinalityOptions.asInt(cardinality);
	}

	public void setContextClassloader(String classLoaderManagementOption) {
		this.contextClassloader = ReferenceClassLoadingOptions.getFromString(classLoaderManagementOption);
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

	/**
	 * How long should we wait between failed attempts at rebinding to a service
	 * that has been unregistered. <p/>
	 * 
	 * @param millisBetweenRetries The millisBetweenRetries to set.
	 */
	public void setTimeout(long millisBetweenRetries) {
		this.retryTemplate.setWaitTime(millisBetweenRetries);
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
