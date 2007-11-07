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
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.internal.service.BeanNameServicePropertiesResolver;
import org.springframework.osgi.internal.service.importer.AbstractServiceImporter;
import org.springframework.osgi.internal.util.ClassUtils;
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
public abstract class AbstractOsgiServiceProxyFactoryBean extends AbstractServiceImporter implements SmartFactoryBean,
		InitializingBean, DisposableBean, BundleContextAware, BeanClassLoaderAware {

	private static final Log log = LogFactory.getLog(AbstractOsgiServiceProxyFactoryBean.class);

	protected ClassLoader classLoader;

	protected BundleContext bundleContext;

	protected int contextClassloader = ReferenceClassLoadingOptions.CLIENT.shortValue();

	// not required to be an interface, but usually should be...
	protected Class[] interfaces;

	// filter used to narrow service matches, may be null
	protected String filter;

	// Cumulated filter string between the specified classes/interfaces and the
	// given filter
	protected Filter unifiedFilter;

	// service lifecycle listener
	protected OsgiServiceLifecycleListener[] listeners;

	/** Service Bean property of the OSGi service * */
	protected String serviceBeanName;

	protected boolean initialized = false;

	/**
	 * Subclasses have to implement this method and return the appropriate
	 * service proxy.
	 */
	public abstract Object getObject();

	/**
	 * Subclasses have to implement this method and return the appropriate
	 * service proxy type.
	 */
	public abstract Class getObjectType();

	public boolean isSingleton() {
		return true;
	}

	public boolean isEagerInit() {
		return true;
	}

	public boolean isPrototype() {
		return false;
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.bundleContext, "Required 'bundleContext' property was not set");
		Assert.notNull(classLoader, "Required 'classLoader' property was not set");
		Assert.notNull(interfaces, "Required 'interfaces' property was not set");
		// validate specified classes
		Assert.isTrue(!ClassUtils.containsUnrelatedClasses(interfaces),
			"more then one concrete class specified; cannot create proxy");

		this.listeners = (listeners == null ? new OsgiServiceLifecycleListener[0] : listeners);

		getUnifiedFilter(); // eager initialization of the cache to catch filter
		// errors
		Assert.notNull(interfaces, "Required serviceTypes property not specified");

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

		String filterWithClasses = OsgiFilterUtils.unifyFilter(interfaces, filter);

		if (log.isTraceEnabled())
			log.trace("unified classes=" + ObjectUtils.nullSafeToString(interfaces) + " and filter=[" + filter
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
	 * Determine the classloader to set.
	 * 
	 * @param reference
	 * @param contextClassLoader
	 * @return
	 */
	// FIXME: this should be moved outside the class
	protected ClassLoader determineClassLoader(ServiceReference reference, int contextClassLoader,
			ClassLoader clientClassLoader) {
		boolean trace = log.isTraceEnabled();

		if (ReferenceClassLoadingOptions.CLIENT.shortValue() == contextClassLoader) {
			if (trace) {
				log.trace("client TCCL used for this invocation");
			}
			return clientClassLoader;
		}
		else if (ReferenceClassLoadingOptions.SERVICE_PROVIDER.shortValue() == contextClassLoader) {
			if (trace) {
				log.trace("service provider TCCL used for this invocation");
			}

			return BundleDelegatingClassLoader.createBundleClassLoaderFor(reference.getBundle());
		}
		else if (ReferenceClassLoadingOptions.UNMANAGED.shortValue() == contextClassLoader) {
			if (trace) {
				log.trace("no (unmanaged)TCCL used for this invocation");
			}
		}
		return null;
	}

	/**
	 * The type that the OSGi service was registered with.
	 * 
	 * @deprecated use {@link #setInterfaces(Class[])}. this method will be
	 * removed after RC1.
	 */
	public void setInterface(Class[] serviceType) {
		this.interfaces = serviceType;
	}

	public void setInterfaces(Class[] serviceType) {
		this.interfaces = serviceType;
	}

	public void setContextClassloader(String classLoaderManagementOption) {
		this.contextClassloader = ReferenceClassLoadingOptions.resolveEnum(classLoaderManagementOption).shortValue();
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
	public void setListeners(OsgiServiceLifecycleListener[] listeners) {
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

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

}
