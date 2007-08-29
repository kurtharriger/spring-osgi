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
 */
package org.springframework.osgi.service.exporter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Constants;
import org.springframework.core.Ordered;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.OsgiBundleScope;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.BeanNameServicePropertiesResolver;
import org.springframework.osgi.service.OsgiServicePropertiesResolver;
import org.springframework.osgi.service.interceptor.OsgiServiceTCCLInvoker;
import org.springframework.osgi.util.ClassUtils;
import org.springframework.osgi.util.MapBasedDictionary;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * FactoryBean that transparently publishes other beans in the same application
 * context as OSGi services returning the ServiceRegistration for the given
 * object.
 * 
 * <p/> The service properties used when publishing the service are determined
 * by the OsgiServicePropertiesResolver. The default implementation uses
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
 */
public class OsgiServiceFactoryBean implements BeanFactoryAware, InitializingBean, DisposableBean, BundleContextAware,
		FactoryBean, Ordered, BeanClassLoaderAware {

	/**
	 * ServiceFactory used for publishing the service beans. Acts as a a wrapper
	 * around special beans (such as ServiceFactory) and delegates to the
	 * container each time a bundle requests the service for the first time.
	 * 
	 */
	private class PublishingServiceFactory implements ServiceFactory {

		// used if the published bean is itself a ServiceFactory
		private ServiceFactory serviceFactory;

		private Class[] classes;

		protected PublishingServiceFactory(Class[] classes) {
			this.classes = classes;
		}

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

			if (contextClassloaderManagementStrategy == ExportClassLoadingOptions.SERVICE_PROVIDER) {
				return wrapWithClassLoaderManagingProxy(bean, classes);
			}
			else {
				return bean;
			}
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

	/**
	 * Decorating {@link org.osgi.framework.ServiceFactory} used for supporting
	 * 'bundle' scoped beans.
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class BundleScopeServiceFactory implements ServiceFactory {
		private ServiceFactory decoratedServiceFactory;

		private Runnable destructionCallback;

		private Class[] classes;

		public BundleScopeServiceFactory(ServiceFactory serviceFactory, Class[] classes) {
			Assert.notNull(serviceFactory);
			this.decoratedServiceFactory = serviceFactory;
			this.classes = classes;
		}

		public Object getService(Bundle bundle, ServiceRegistration registration) {
			// inform OsgiBundleScope (just place a boolean)
			OsgiBundleScope.CALLING_BUNDLE.set(Boolean.TRUE);
			try {
				Object obj = decoratedServiceFactory.getService(bundle, registration);
				// retrieve destructionCallback if any
				Object callback = OsgiBundleScope.CALLING_BUNDLE.get();
				if (callback != null && callback instanceof Runnable)
					this.destructionCallback = (Runnable) callback;
				if (contextClassloaderManagementStrategy == ExportClassLoadingOptions.SERVICE_PROVIDER) {
					obj = wrapWithClassLoaderManagingProxy(obj, classes);
				}
				return obj;
			}
			finally {
				// clean ThreadLocal
				OsgiBundleScope.CALLING_BUNDLE.set(null);
			}
		}

		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			decoratedServiceFactory.ungetService(bundle, registration, service);
			if (destructionCallback != null)
				destructionCallback.run();
		}

	}

	private static final Log log = LogFactory.getLog(OsgiServiceFactoryBean.class);

	public static final int AUTO_EXPORT_DISABLED = 0;

	public static final int AUTO_EXPORT_INTERFACES = 1;

	public static final int AUTO_EXPORT_CLASS_HIERARCHY = 2;

	public static final int AUTO_EXPORT_ALL = AUTO_EXPORT_INTERFACES | AUTO_EXPORT_CLASS_HIERARCHY;

	private static final String AUTO_EXPORT_PREFIX = "AUTO_EXPORT_";

	private static final Constants EXPORTING_OPTIONS = new Constants(OsgiServiceFactoryBean.class);

	private BundleContext bundleContext;

	private OsgiServicePropertiesResolver propertiesResolver;

	private BeanFactory beanFactory;

	private ServiceRegistration serviceRegistration;

	private Map serviceProperties;

	private int ranking;

	private String targetBeanName;

	private Class[] interfaces;

	private int autoExportMode = AUTO_EXPORT_DISABLED;

	private String activationMethod;

	private String deactivationMethod;

	private int contextClassloaderManagementStrategy = ExportClassLoadingOptions.UNMANAGED;

	private Object target;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private ClassLoader classLoader;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(beanFactory, "required property 'beanFactory' has not been set");
		Assert.notNull(bundleContext, "required property 'bundleContext' has not been set");

		Assert.notNull(target, "'target' property is required");

		if (propertiesResolver == null) {
			propertiesResolver = new BeanNameServicePropertiesResolver();
			((BeanNameServicePropertiesResolver) propertiesResolver).setBundleContext(bundleContext);
		}

		// sanity check
		if (interfaces == null)
			interfaces = new Class[0];

		Class serviceClass = target.getClass();
		// if we have a nested bean / non-Spring managed object
		String beanName = (!StringUtils.hasText(targetBeanName) ? ObjectUtils.getIdentityHexString(target)
				: targetBeanName);
		publishService(serviceClass, mergeServiceProperties(beanName));
	}

	/**
	 * Proxy the target object with a proxy that manages the context
	 * classloader.
	 * 
	 * @param target
	 * @return
	 */
	private Object wrapWithClassLoaderManagingProxy(final Object target, Class[] interfaces) {
		ProxyFactory factory = new ProxyFactory();

		// mold the proxy
		for (int i = 0; i < interfaces.length; i++) {
			factory.addInterface(interfaces[i]);
		}

		factory.addAdvice(new OsgiServiceTCCLInvoker(target, classLoader));

		try {
			return factory.getProxy(classLoader);
		}
		catch (NoClassDefFoundError ncdfe) {
			if (log.isWarnEnabled()) {
				debugClassLoading(ncdfe);
			}
			throw ncdfe;
		}
	}

	/**
	 * Try and figure out why the proxy generation failed.
	 * 
	 * @param ncdfe
	 */
	protected void debugClassLoading(NoClassDefFoundError ncdfe) {
		String cname = ncdfe.getMessage().replace('/', '.');
		BundleDelegatingClassLoader.debugClassLoading(bundleContext.getBundle(), cname, null);
		// Check out all the classes.
		for (int i = 0; i < interfaces.length; i++) {
			ClassLoader cl = interfaces[i].getClassLoader();
			String cansee = "cannot";
			try {
				cl.loadClass(cname);
				cansee = "can";
			}
			catch (Exception e) {
				// ignored
			}
			log.warn(interfaces[i].toString() + " is loaded by " + cl.toString() + " which " + cansee + " see " + cname);
		}
	}

	private Dictionary mergeServiceProperties(String beanName) {
		MapBasedDictionary props = new MapBasedDictionary(propertiesResolver.getServiceProperties(beanName));

		props.putAll((Map) props);

		// add service properties
		if (serviceProperties != null)
			props.putAll(serviceProperties);

		if (ranking != 0) {
			props.put(org.osgi.framework.Constants.SERVICE_RANKING, new Integer(ranking));
		}
		return props;
	}

	/**
	 * Return an array of classes for the given bean that have been discovered
	 * using the autoExportMode.
	 * 
	 * @param clazz
	 * @return
	 */
	protected Class[] autoDetectClassesForPublishing(Class clazz) {

		Class[] classes;

		switch (autoExportMode) {
		case AUTO_EXPORT_ALL:
			classes = org.springframework.osgi.util.ClassUtils.getClassHierarchy(clazz,
				org.springframework.osgi.util.ClassUtils.INCLUDE_ALL_CLASSES);
			break;
		case AUTO_EXPORT_CLASS_HIERARCHY:
			classes = org.springframework.osgi.util.ClassUtils.getClassHierarchy(clazz,
				org.springframework.osgi.util.ClassUtils.INCLUDE_CLASS_HIERARCHY);
			break;
		case AUTO_EXPORT_INTERFACES:
			classes = org.springframework.osgi.util.ClassUtils.getClassHierarchy(clazz,
				org.springframework.osgi.util.ClassUtils.INCLUDE_INTERFACES);
			break;
		default:
			classes = new Class[0];
			break;
		}
		if (log.isTraceEnabled())
			log.trace("autodetect mode [" + autoExportMode + "] discovered on class [" + clazz + "] classes "
					+ ObjectUtils.nullSafeToString(classes));

		return classes;
	}

	/**
	 * Publish the given object as an OSGi service. It simply assembles the
	 * classes required for publishing and then delegates the actual
	 * registration to a dedicated method.
	 * 
	 * @param beanClass
	 * @param serviceProperties
	 */
	protected void publishService(Class beanClass, Dictionary serviceProperties) {
		Class[] intfs = interfaces;
		Class[] autoDetectedClasses = autoDetectClassesForPublishing(beanClass);

		// filter duplicates
		Set classes = new LinkedHashSet(intfs.length + autoDetectedClasses.length);

		CollectionUtils.mergeArrayIntoCollection(intfs, classes);
		CollectionUtils.mergeArrayIntoCollection(autoDetectedClasses, classes);

		Class[] mergedClasses = (Class[]) classes.toArray(new Class[classes.size()]);

		serviceRegistration = registerService(mergedClasses, serviceProperties);
	}

	/**
	 * Registration method.
	 * 
	 * @param classes
	 * @param serviceProperties
	 * @return the ServiceRegistration
	 */
	protected ServiceRegistration registerService(Class[] classes, Dictionary serviceProperties) {
		Assert.notEmpty(
			classes,
			"at least one class has to be specified for exporting (if autoExport is enabled then maybe the object doesn't implement any interface)");

		Class beanClass = (target == null ? beanFactory.getType(targetBeanName)
				: target.getClass());
		
		// filter classes based on visibility
		ClassLoader beanClassLoader = ClassUtils.getClassLoader(beanClass);
		
		Class[] visibleClasses = ClassUtils.getVisibleClasses(classes, beanClassLoader);

		// create an array of classnames (used for registering the service)
		String[] names = org.springframework.osgi.util.ClassUtils.toStringArray(visibleClasses);

		// sort the names in alphabetical order (eases debugging)
		Arrays.sort(names);

		log.info("Publishing service under classes [" + ObjectUtils.nullSafeToString(names) + "]");

		ServiceFactory serviceFactory = new PublishingServiceFactory(visibleClasses);

		if (isBeanBundleScoped())
			serviceFactory = new BundleScopeServiceFactory(serviceFactory, visibleClasses);

		return bundleContext.registerService(names, serviceFactory, serviceProperties);
	}

	protected boolean isBeanBundleScoped() {
		boolean bundleScoped = false;
		// if we do have a bundle scope, use ServiceFactory decoration
		if (targetBeanName != null) {
			if (beanFactory instanceof ConfigurableListableBeanFactory) {
				String beanScope = ((ConfigurableListableBeanFactory) beanFactory).getMergedBeanDefinition(
					targetBeanName).getScope();
				bundleScoped = OsgiBundleScope.SCOPE_NAME.equals(beanScope);
			}
			else
				// if for some reason, the passed in BeanFactory can't be
				// queried for scopes and we do
				// have a bean reference, apply scoped decoration.
				bundleScoped = true;
		}
		return bundleScoped;
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

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public Object getObject() throws Exception {
		return serviceRegistration;
	}

	public Class getObjectType() {
		return (serviceRegistration != null ? serviceRegistration.getClass() : ServiceRegistration.class);
	}

	public boolean isSingleton() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() {
		// stop published service
		unregisterService(serviceRegistration);
	}

	/**
	 * Set the context classloader management strategy to use when invoking
	 * operations on the exposed target bean
	 * @param classloaderManagementOption
	 */
	public void setContextClassloader(String classloaderManagementOption) {
		this.contextClassloaderManagementStrategy = ExportClassLoadingOptions.getFromString(classloaderManagementOption);
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

	public Map getServiceProperties() {
		return serviceProperties;
	}

	public void setServiceProperties(Map serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

	public int getRanking() {
		return ranking;
	}

	public void setRanking(int ranking) {
		this.ranking = ranking;
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
		return interfaces;
	}

	public void setInterfaces(Class[] serviceInterfaces) {
		this.interfaces = serviceInterfaces;
	}

	/**
	 * @return Returns the order.
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * @param order The order to set.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
