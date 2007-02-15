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
 *
 * Created on 23-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;
import org.springframework.core.Constants;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A bean that transparently publishes other beans in the same application
 * context as OSGi services. <p/> The service properties used when publishing
 * the service are determined by the OsgiServicePropertiesResolver. The default
 * implementation uses
 * <ul>
 * <li>BundleSymbolicName=&lt;bundle symbolic name&gt;</li>
 * <li>BundleVersion=&lt;bundle version&gt;</li>
 * <li>org.springframework.osgi.beanname="&lt;bean name&gt;</li>
 * </ul>
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 * @author Hal Hildebrand
 * @author Andy Piper
 * 
 * @since 1.0
 */
public class OsgiServiceExporter implements BeanFactoryAware, InitializingBean, DisposableBean, BundleContextAware {

	/**
	 * ServiceFactory used for posting the bean instantiation until it is first
	 * needed.
	 * 
	 */
	private class LazyBeanServiceFactory implements ServiceFactory {

		// used if the published bean is itself a ServiceFactory
		private ServiceFactory serviceFactory;

		public Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {

			Object bean = (target != null ? target : beanFactory.getBean(targetBeanName));

			// if we get a ServiceFactory, call its method
			if (bean instanceof ServiceFactory) {
				serviceFactory = (ServiceFactory) bean;
				bean = serviceFactory.getService(bundle, serviceRegistration);
			}

			// FIXME: what's the spec on this one?
			if (StringUtils.hasText(activationMethod)) {
				Method m = BeanUtils.resolveSignature(activationMethod, bean.getClass());
				try {
					ReflectionUtils.invokeMethod(m, bean);
				}
				catch (RuntimeException ex) {
					log.error("Activation method for [" + bean + "] threw an exception", ex);
				}
			}
			return bean;
		}

		public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object bean) {
			// FIXME: what's the spec on this one?
			if (StringUtils.hasText(deactivationMethod)) {
				Method m = BeanUtils.resolveSignature(deactivationMethod, bean.getClass());
				try {
					ReflectionUtils.invokeMethod(m, bean);
				}
				catch (RuntimeException ex) {
					log.error("Deactivation method for [" + bean + "] threw an exception", ex);
				}
			}

			if (serviceFactory != null)
				serviceFactory.ungetService(bundle, serviceRegistration, bean);
		}
	}

	private static final Log log = LogFactory.getLog(OsgiServiceExporter.class);

	public static final int AUTO_EXPORT_DISABLED = 0;

	public static final int AUTO_EXPORT_INTERFACES = 1;

	public static final int AUTO_EXPORT_CLASS_HIERARCHY = 2;

	public static final int AUTO_EXPORT_ALL = AUTO_EXPORT_INTERFACES | AUTO_EXPORT_CLASS_HIERARCHY;

	private static final String AUTO_EXPORT_PREFIX = "AUTO_EXPORT_";

	private static final Constants EXPORTING_OPTIONS = new Constants(OsgiServiceExporter.class);

	private BundleContext bundleContext;

	private OsgiServicePropertiesResolver propertiesResolver;

	private BeanFactory beanFactory;

	private ServiceRegistration publishedService;

	private Properties serviceProperties;

	private String targetBeanName;

	private Class[] interfaces;

	private int autoExportMode = AUTO_EXPORT_DISABLED;

	// TODO: what are these?
	private String activationMethod;

	private String deactivationMethod;

	private int contextClassloaderManagementStrategy;

	private Object target;

	private Class[] defensiveCopyOf(Class[] original) {
		if (null == original) {
			return new Class[0];
		}
		else {
			Class[] copy = new Class[original.length];
			System.arraycopy(original, 0, copy, 0, original.length);
			return copy;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(beanFactory, "required property 'beanFactory' has not been set");
		Assert.notNull(bundleContext, "required property 'bundleContext' has not been set");

		if (targetBeanName == null && target == null || (targetBeanName != null && target != null))
			throw new IllegalArgumentException("either 'target' or 'targetBeanName' have to be specified");

		if (propertiesResolver == null) {
			propertiesResolver = new BeanNameServicePropertiesResolver();
			((BeanNameServicePropertiesResolver) propertiesResolver).setBundleContext(bundleContext);
		}

		// sanity check
		if (interfaces == null)
			interfaces = new Class[0];

		// determine serviceClass (can still be null if using a FactoryBean which doesn't declare its product type)
		Class serviceClass = (target != null ? target.getClass() : beanFactory.getType(targetBeanName));

		if (this.contextClassloaderManagementStrategy == ExportClassLoadingOptions.SERVICE_PROVIDER) {
			// FIXME: add classloading wrappers
			wrapWithClassLoaderManagingProxy(serviceClass, interfaces);
		}

		// if we have a nested bean / non-Spring managed object
		String beanName = (targetBeanName == null ? ObjectUtils.getIdentityHexString(target) : targetBeanName);
		publishService(serviceClass, mergeServiceProperties(beanName));
	}

	/**
	 * Proxy the target object with a proxy that manages the context
	 * classloader.
	 * 
	 * @param target2
	 * @return
	 */
	private Object wrapWithClassLoaderManagingProxy(Class serviceType, Class[] interfaces) {
		
		// TODO implement wrapping, take into account that toBeProxied may
		// *already* be advised...
		return new Object();
	}

	private Properties mergeServiceProperties(String beanName) {
		Properties p = propertiesResolver.getServiceProperties(beanName);
		if (serviceProperties != null) {
			p.putAll(serviceProperties);
		}
		return p;
	}

	/**
	 * Returns true if a given mode is enabled for this exporter instance.
	 * 
	 * @param mode
	 * @return
	 */
	private boolean isAutoExportModeEnabled(int mode) {
		return (mode == AUTO_EXPORT_DISABLED ? autoExportMode == AUTO_EXPORT_DISABLED : (autoExportMode & mode) == mode);
	}

	/**
	 * Return an array of classes for the given bean that have been discovered
	 * using the autoExportMode.
	 * 
	 * @param bean
	 * @return
	 */
	protected Class[] autoDetectClassesForPublishing(Class clazz) {

		Class[] classes = new Class[0];

		if (!isAutoExportModeEnabled(AUTO_EXPORT_DISABLED)) {
			if (clazz == null) {
				log.debug("service type is null - skipping autodetection");
				return classes;
			}
			else {
				Set composingClasses = CollectionFactory.createLinkedSetIfPossible(16);

				if (isAutoExportModeEnabled(AUTO_EXPORT_INTERFACES))
					composingClasses.addAll(ClassUtils.getAllInterfacesForClassAsSet(clazz));

				if (isAutoExportModeEnabled(AUTO_EXPORT_CLASS_HIERARCHY)) {
					Class clz = clazz;
					do {
						composingClasses.add(clz);
						clz = clz.getSuperclass();
					} while (clz != null && clz != Object.class);
				}

				classes = (Class[]) composingClasses.toArray(new Class[composingClasses.size()]);

				if (log.isDebugEnabled())
					log.debug("autodetect mode [" + autoExportMode + "] discovered on class [" + clazz + "] classes "
							+ Arrays.toString(classes));
			}
		}

		return classes;
	}

	/**
	 * Publish the given object as an OSGi service. It simply assembles the
	 * classes required for publishing and then delegates the actual
	 * registration to a dedicated method.
	 * 
	 * @param bean
	 * @param serviceProperties
	 */
	protected void publishService(Class beanClass, Properties serviceProperties) {
		Class[] intfs = interfaces;
		Class[] autoDetectedClasses = autoDetectClassesForPublishing(beanClass);

		// filter duplicates
		Set classes = CollectionFactory.createLinkedSetIfPossible(intfs.length + autoDetectedClasses.length);
		Collections.addAll(classes, intfs);
		Collections.addAll(classes, autoDetectedClasses);

		Class[] publishingClasses = (Class[]) classes.toArray(new Class[classes.size()]);

		publishedService = registerService(publishingClasses, serviceProperties);
	}

	/**
	 * Registration method.
	 * 
	 * @param classes
	 * @param bean
	 * @param serviceProperties
	 * @return
	 */
	protected ServiceRegistration registerService(Class[] classes, Properties serviceProperties) {
		// create an array of classnames (used for registering the service)
		String[] names = new String[classes.length];

		for (int i = 0; i < classes.length; i++) {
			names[i] = classes[i].getName();
		}
		// sort the names in alphabetical order (eases debugging)
		Arrays.sort(names);

		log.info("Publishing service under classes [" + Arrays.toString(names) + "]");

		return bundleContext.registerService(names, new LazyBeanServiceFactory(), serviceProperties);
	}

	/**
	 * Unregisters (literally stops) a service.
	 * 
	 * @param registration
	 */
	protected void unregisterService(ServiceRegistration registration) {
		if (registration != null)
			try {
				registration.unregister();
			}
			catch (IllegalStateException ise) {
				// Service was already unregistered, probably because the bundle
				// was stopped.
				if (log.isInfoEnabled()) {
					log.info("Service [" + registration + "] has already been unregistered");
				}
			}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() {
		// stop published service
		unregisterService(publishedService);
	}

	/**
	 * Set the context classloader management strategy to use when invoking
	 * operations on the exposed target bean
	 * @param classloaderManagementOption
	 */
	public void setContextClassloader(String classloaderManagementOption) {
		this.contextClassloaderManagementStrategy = ExportClassLoadingOptions
				.getFromString(classloaderManagementOption);
	}

	/**
	 * Export the given object as an OSGi service. Normally used when the
	 * exported service is a nested bean or an object not managed by the Spring
	 * container.
	 * 
	 * @param target The target to set.
	 */
	public void setTarget(Object target) {
		this.target = target;
	}

	/**
	 * Set the autoexport mode to use. This allows the exporter to use the
	 * target class hierarchy and/or interfaces for registering the OSGi
	 * service. By default, autoExport is disabled (@link
	 * {@link #AUTO_EXPORT_DISABLED}
	 * 
	 * @see #setAutoExportName(String)
	 * @see #AUTO_EXPORT_DISABLED
	 * @see #AUTO_EXPORT_INTERFACES
	 * @see #AUTO_EXPORT_CLASS_HIERARCHY
	 * @see #AUTO_EXPORT_ALL
	 * 
	 * @param autoExportMode the auto export mode as an int
	 */
	public void setAutoExportNumber(int autoExportMode) {
		if (!EXPORTING_OPTIONS.getValues(AUTO_EXPORT_PREFIX).contains(new Integer(autoExportMode)))
			throw new IllegalArgumentException("invalid autoExportMode");
		this.autoExportMode = autoExportMode;
	}

	/**
	 * Set the autoexport mode to use.
	 * @see #setAutoExportNumber(int)
	 * 
	 * @param autoExportMode
	 */
	public void setAutoExport(String autoExportMode) {
		if (autoExportMode != null) {
			if (!autoExportMode.startsWith(AUTO_EXPORT_PREFIX))
				autoExportMode = AUTO_EXPORT_PREFIX + autoExportMode;
			this.autoExportMode = EXPORTING_OPTIONS.asNumber(autoExportMode).intValue();
		}
	}

	public String getActivationMethod() {
		return activationMethod;
	}

	public void setActivationMethod(String activationMethod) {
		this.activationMethod = activationMethod;
	}

	public String getDeactivationMethod() {
		return deactivationMethod;
	}

	public void setDeactivationMethod(String deactivationMethod) {
		this.deactivationMethod = deactivationMethod;
	}

	public Properties getServiceProperties() {
		return serviceProperties;
	}

	public void setServiceProperties(Properties serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}

	/**
	 * @return Returns the resolver.
	 */
	public OsgiServicePropertiesResolver getResolver() {
		return this.propertiesResolver;
	}

	/**
	 * @param resolver The resolver to set.
	 */
	public void setResolver(OsgiServicePropertiesResolver resolver) {
		this.propertiesResolver = resolver;
	}

	public void setTargetBeanName(String name) {
		this.targetBeanName = name;
	}

	public Class[] getInterfaces() {
		return defensiveCopyOf(this.interfaces);
	}

	public void setInterfaces(Class[] serviceInterfaces) {
		this.interfaces = defensiveCopyOf(serviceInterfaces);
	}
}
