/*
 * Copyright 2002-2007 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.BeanNameServicePropertiesResolver;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.osgi.util.ClassUtils;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Base class for importing OSGi services. Provides most of the constructs
 * required for assembling the service proxies, leaving subclasses to decide on
 * the service cardinality (one service or multiple).
 * 
 * @author Costin Leau
 * @author Adrian Colyer
 * @author Hal Hildebrand
 * 
 */
public abstract class AbstractOsgiServiceProxyFactoryBean implements FactoryBean, InitializingBean, DisposableBean,
		BundleContextAware, ApplicationListener, BeanClassLoaderAware {

	private static final Log log = LogFactory.getLog(AbstractOsgiServiceProxyFactoryBean.class);

	public static final String FILTER_ATTRIBUTE = "filter";

	public static final String INTERFACE_ATTRIBUTE = "interface";

	public static final String CARDINALITY_ATTRIBUTE = "cardinality";

	public static final String OBJECTCLASS = "objectClass";

	protected ClassLoader classLoader;

	protected BundleContext bundleContext;

	protected int contextClassloader = ReferenceClassLoadingOptions.CLIENT;

	/** is at least one service required? * */
	protected boolean mandatory = true;

	// not required to be an interface, but usually should be...
	protected Class[] serviceTypes;

	// filter used to narrow service matches, may be null
	protected String filter;

	// Cumulated filter string between the specified classes/interfaces and the
	// given filter
	protected Filter unifiedFilter;

	// service lifecycle listener
	protected TargetSourceLifecycleListener[] listeners = new TargetSourceLifecycleListener[0];

	/** Service Bean property of the OSGi service * */
	protected String serviceBeanName;

	protected boolean initialized = false;

	// FIXME: what is this used for ?
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if (applicationEvent instanceof ContextRefreshedEvent) {
			// This sets up the listeners for beans which are not referred to by
			// any other
			// bean in the context. We can't do this in afterPropertiesSet, so
			// we have to do
			// it here.
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(classLoader);
				getObject();
			}
			finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}
		}
	}

	/**
	 * Subclasses have to implement this method and return the approapriate
	 * service proxy.
	 */
	public abstract Object getObject();

	/**
	 * Subclasses have to implement this method and return the approapriate
	 * service proxy type.
	 */

	public abstract Class getObjectType();

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
	public void afterPropertiesSet() {
		Assert.notNull(this.bundleContext, "Required bundleContext property was not set");
		Assert.notNull(classLoader, "Required classLoader property was not set");
		Assert.notNull(serviceTypes, "Required serviceTypes property was not set");
		// validate specified classes
		Assert.isTrue(!ClassUtils.containsUnrelatedClasses(serviceTypes),
			"more then one concrete class specified; cannot create proxy");
		getUnifiedFilter(); // eager initialization of the cache to catch filter
		// errors
		Assert.notNull(serviceTypes, "Required serviceTypes property not specified");
		initialized = true;
	}

	/**
	 * Assemble configuration properties to create an OSGi filter. Manages
	 * internally the unifiedFilter field as a cache, so the filter creation
	 * happens only once.
	 * 
	 * @return osgi filter
	 */
	public Filter getUnifiedFilter() {
		if (unifiedFilter != null) {
			return unifiedFilter;
		}

		// clean up parent classes
		serviceTypes = ClassUtils.removeParents(serviceTypes);

		String filterWithClasses = OsgiFilterUtils.unifyFilter(serviceTypes, filter);

		if (log.isTraceEnabled())
			log.trace("unified classes=" + ObjectUtils.nullSafeToString(serviceTypes) + " and filter=[" + filter
					+ "]  in=[" + filterWithClasses + "]");

		// add the serviceBeanName constraint
		String filterWithServiceBeanName = OsgiFilterUtils.unifyFilter(
			BeanNameServicePropertiesResolver.BEAN_NAME_PROPERTY_KEY.toString(), new String[] { serviceBeanName },
			filterWithClasses);

		if (log.isTraceEnabled())
			log.trace("unified serviceBeanName [" + ObjectUtils.nullSafeToString(serviceBeanName) + "] and filter=["
					+ filterWithClasses + "]  in=[" + filterWithServiceBeanName + "]");

		// create (which implies validation) the actual filter
		unifiedFilter = OsgiFilterUtils.createFilter(filterWithServiceBeanName);

		return unifiedFilter;
	}

	public abstract void destroy() throws Exception;

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
	 * Try and figure out why the proxy generation failed.
	 * 
	 * @param ncdfe
	 */
	// FIXME: move this somewhere else
	protected void debugClassLoading(NoClassDefFoundError ncdfe) {
		String cname = ncdfe.getMessage().replace('/', '.');
		BundleDelegatingClassLoader.debugClassLoading(bundleContext.getBundle(), cname, null);
		// Check out all the classes.
		for (int i = 0; i < serviceTypes.length; i++) {
			ClassLoader cl = serviceTypes[i].getClassLoader();
			String cansee = "cannot";
			try {
				cl.loadClass(cname);
				cansee = "can";
			}
			catch (Exception e) {
				// ignored
			}
			log.warn(serviceTypes[i].toString() + " is loaded by " + cl.toString() + " which " + cansee + " see "
					+ cname);
		}
	}

	/**
	 * The type that the OSGi service was registered with
	 */
	public void setInterface(Class[] serviceType) {
		this.serviceTypes = serviceType;
	}

	public void setContextClassloader(String classLoaderManagementOption) {
		this.contextClassloader = ReferenceClassLoadingOptions.getFromString(classLoaderManagementOption);
	}

	public void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}

	/**
	 * @param filter The filter to set.
	 */
	public void setFilter(String filter) {
		this.filter = filter;
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

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	public boolean isMandatory() {
		return mandatory;
	}
	
}
