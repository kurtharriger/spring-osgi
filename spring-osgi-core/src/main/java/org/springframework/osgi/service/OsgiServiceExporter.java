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
import java.util.HashSet;
import java.util.Iterator;
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
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
 * @since 2.0
 */
public class OsgiServiceExporter implements BeanFactoryAware, InitializingBean, DisposableBean,
		BundleContextAware
{

	/**
	 * ClassLoaderOptions Constants Class.
	 *
	 * @author Costin Leau
	 */
	protected class ClassLoaderOptions
	{
		public static final int UNMANAGED = 0;
		public static final int SERVICE_PROVIDER = 1;
	}

	private static final Log log = LogFactory.getLog(OsgiServiceExporter.class);

	private BundleContext bundleContext;

	private OsgiServicePropertiesResolver resolver = new BeanNameServicePropertiesResolver();
	private BeanFactory beanFactory;
	private Set publishedServices = new HashSet();

	private Properties serviceProperties;
	private String targetBeanName = OsgiServiceExporter.class.getName();
	private Class[] interfaces;

	// TODO: what are these?
	private String activationMethod;
	private String deactivationMethod;

	private static final Constants CL_OPTIONS = new Constants(ClassLoaderOptions.class);

	private int contextClassloader;
	private Object target;

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
		// REVIEW andyp -- when coming through the ContextLoaderListener this
		// seems necessary.
		if (resolver instanceof BundleContextAware) {
			((BundleContextAware) resolver).setBundleContext(context);
		}
	}

	/**
	 * @return Returns the resolver.
	 */
	public OsgiServicePropertiesResolver getResolver() {
		return this.resolver;
	}

	/**
	 * @param resolver The resolver to set.
	 */
	public void setResolver(OsgiServicePropertiesResolver resolver) {
		this.resolver = resolver;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setTargetBeanName(String name) {
		this.targetBeanName = name;
	}

	public Class[] getInterfaces() {
		return interfaces;
	}

	public void setInterfaces(Class[] serviceInterface) {
		this.interfaces = serviceInterface;
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
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(beanFactory, "required property 'beanFactory' has not been set");
		Assert.notNull(bundleContext, "required property 'bundleContext' has not been set");
		Assert.notNull(resolver, "required property 'resolver' was set to a null value");
		Assert.notNull(target, "required property 'target' has not been set");

		publishService(target, mergeServiceProperties(targetBeanName));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		for (Iterator iter = this.publishedServices.iterator(); iter.hasNext();) {
			ServiceRegistration sReg = (ServiceRegistration) iter.next();
			try {
				sReg.unregister();
			}
			catch (IllegalStateException ise) {
				// Service was already unregistered, probably because the bundle
				// was stopped.
				if (log.isInfoEnabled()) {
					log.info("Service [" + sReg + "] has already been unregistered");
				}
			}
		}
	}

	private Properties mergeServiceProperties(String beanName) {
		Properties p = resolver.getServiceProperties(beanName);
		if (serviceProperties != null) {
			p.putAll(serviceProperties);
		}
		return p;
	}

	protected void publishService(Object bean, Properties serviceProperties) {
		Class[] intfs = interfaces;

		// detect interfaces (if necessary)
		if (intfs == null || intfs.length == 0)
			intfs = ClassUtils.getAllInterfaces(bean);

		// fallback to classname
		if (intfs == null || intfs.length == 0)
			intfs = new Class[]{bean.getClass()};

		publishService(intfs, bean, serviceProperties);
	}

	protected ServiceRegistration publishService(Class[] intfs, Object bean, Properties serviceProperties) {
		String[] names = new String[intfs.length];

		for (int i = 0; i < names.length; i++) {
			names[i] = intfs[i].getName();
		}

		if (log.isInfoEnabled()) {
			log.info("Publishing service [" + Arrays.toString(names) + "]");
		}
		// Service registration optionally proxied to avoid eager creation
		ServiceRegistration s;

		if (!targetBeanName.equals(getClass().getName()) &&
				(beanFactory.containsBean(BeanFactory.FACTORY_BEAN_PREFIX + targetBeanName)
						|| (beanFactory instanceof BeanDefinitionRegistry)
						&& ((BeanDefinitionRegistry) beanFactory).getBeanDefinition(targetBeanName).isLazyInit())) {
			s = bundleContext.registerService(names, new BeanServiceFactory(targetBeanName), serviceProperties);
		} else {
			s = bundleContext.registerService(names, bean, serviceProperties);
		}
		this.publishedServices.add(s);
		return s;
	}

	private class BeanServiceFactory implements ServiceFactory
	{
		private String beanName;

		public BeanServiceFactory(String beanName) {
			this.beanName = beanName;
		}

		// TODO: reworks this
		public Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {
			Object bean = beanFactory.getBean(beanName);
			if (StringUtils.hasText(activationMethod)) {
				Method m = BeanUtils.resolveSignature(activationMethod, beanFactory.getType(beanName));
				try {
					ReflectionUtils.invokeMethod(m, bean);
				}
				catch (RuntimeException ex) {
					log.error("Activation method on bean with name '" + beanName + "' threw an exception", ex);
				}
			}
			// else if (bean instanceof ServiceActivationLifecycleListener) {
			// ((ServiceActivationLifecycleListener)
			// bean).activate(serviceRegistration.getClass());
			// }
			return bean;
		}

		// TODO: reworks this
		public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object bean) {
			if (StringUtils.hasText(deactivationMethod)) {
				Method m = BeanUtils.resolveSignature(deactivationMethod, beanFactory.getType(beanName));
				try {
					ReflectionUtils.invokeMethod(m, bean);
				}
				catch (RuntimeException ex) {
					log.error("Deactivation method on bean with name '" + beanName + "' threw an exception", ex);
				}
			}
			// else if (bean instanceof ServiceDeactivationLifecycleListener) {
			// ((ServiceDeactivationLifecycleListener)
			// bean).deactivate(serviceRegistration.getClass());
			// }
		}
	}

	// /**
	// * @param contextClassloader The contextClassloader to set.
	// */
	// private void setContextClassloader(int contextClassloader) {
	// if (!CL_OPTIONS.getValues(null).contains(new
	// Integer(contextClassloader)))
	// throw new IllegalArgumentException("illegal constant:" +
	// contextClassloader);
	//
	// this.contextClassloader = contextClassloader;
	// }

	public void setContextClassloader(String classloaderManagementOption) {
		// transform "-" into "_" (for service-provider)
		if (classloaderManagementOption == null)
			throw new IllegalArgumentException("non-null argument required");

		this.contextClassloader = CL_OPTIONS.asNumber(classloaderManagementOption.replace("-", "_")).intValue();
	}

	/**
	 * @return Returns the target.
	 */
	public Object getTarget() {
		return target;
	}

	/**
	 * @param target The target to set.
	 */
	public void setTarget(Object target) {
		this.target = target;
	}

}
