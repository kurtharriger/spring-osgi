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

import java.lang.reflect.InvocationTargetException;
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
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.util.ClassUtils;
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
 * @since 2.0
 */
public class OsgiServiceExporter implements BeanFactoryAware, BeanNameAware, InitializingBean, DisposableBean,
		BundleContextAware {

	protected class ClassLoaderOptions {
		public static final int UNMANAGED = 0;
		public static final int SERVICE_PROVIDER = 1;
	}

	private static final Log log = LogFactory.getLog(OsgiServiceExporter.class);

	private BundleContext bundleContext;
	private OsgiServicePropertiesResolver resolver = new BeanNameServicePropertiesResolver();
	private BeanFactory beanFactory;
	private Set/* <ServiceRegistration> */publishedServices = new HashSet();
	private Properties serviceProperties;
	private String beanName = OsgiServiceExporter.class.getName();
	private String ref;
	private String[] serviceInterface;
	private String activationMethod;
	private String deactivationMethod;

	private static final Constants CL_OPTIONS = new Constants(ClassLoaderOptions.class);

	private int contextClassloader;

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
	public void setBeanName(String name) {
		this.beanName = name;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String[] getInterfaces() {
		return serviceInterface;
	}

	public void setInterfaces(String[] serviceInterface) {
		this.serviceInterface = serviceInterface;
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
		if (this.beanFactory == null) {
			throw new IllegalArgumentException("Required property beanFactory has not been set");
		}
		if (this.bundleContext == null) {
			throw new IllegalArgumentException("Required property bundleContext has not been set");
		}
		if (this.resolver == null) {
			throw new IllegalArgumentException("Required property resolver was set to a null value");
		}
		if (ref == null || ref.length() == 0) {
			throw new IllegalArgumentException("Required property ref has not been set");
		}
		publishBeans();
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

	private void publishBeans() throws NoSuchBeanDefinitionException {
		if (serviceInterface == null || serviceInterface.length == 0) {
			publishBeanAsService(ref, mergeServiceProperties(ref));
		}
		else {
			publishService(serviceInterface, ref, mergeServiceProperties(ref));
		}
	}

	private Properties mergeServiceProperties(String beanName) {
		Properties p = resolver.getServiceProperties(beanName);
		if (serviceProperties != null) {
			p.putAll(serviceProperties);
		}
		return p;
	}

	protected void publishBeanAsService(String bean, Properties serviceProperties) {
		Class[] interfaces = ClassUtils.getAllInterfaces(bean);
		if (interfaces != null && interfaces.length > 0) {
			String[] ifs = new String[interfaces.length];
			for (int i = 0; i < ifs.length; i++) {
				ifs[i] = interfaces[i].getName();
			}
			publishService(ifs, bean, serviceProperties);
		}
		else {
			publishService(new String[] { bean.getClass().getName() }, bean, serviceProperties);
		}
	}

	protected ServiceRegistration publishService(String[] names, String beanName, Properties serviceProperties) {
		if (log.isInfoEnabled()) {
			log.info("Publishing service [" + Arrays.toString(names) + "]");
		}
		// Service registration optionally proxied to avoid eager creation
		ServiceRegistration s;
		if (beanFactory.containsBean("&" + beanName) || (beanFactory instanceof BeanDefinitionRegistry)
				&& ((BeanDefinitionRegistry) beanFactory).getBeanDefinition(beanName).isLazyInit()) {
			s = bundleContext.registerService(names, new BeanServiceFactory(beanName), serviceProperties);
		}
		else {
			s = bundleContext.registerService(names, beanFactory.getBean(beanName), serviceProperties);
		}
		this.publishedServices.add(s);
		return s;
	}

	private class BeanServiceFactory implements ServiceFactory {
		private String beanName;

		public BeanServiceFactory(String beanName) {
			this.beanName = beanName;
		}

		public Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {
			Object bean = beanFactory.getBean(beanName);
			if (StringUtils.hasText(activationMethod)) {
				Method m = BeanUtils.resolveSignature(activationMethod, beanFactory.getType(beanName));
				try {
					m.invoke(bean, null);
				}
				catch (IllegalAccessException e) {
					throw new IllegalArgumentException(e);
				}
				catch (InvocationTargetException e) {
					log.error("Activation method on bean with name '" + beanName + "' threw an exception",
							e.getTargetException());
				}
			}
			else if (bean instanceof ServiceActivationLifecycleListener) {
				((ServiceActivationLifecycleListener) bean).activate(serviceRegistration.getClass());
			}
			return bean;
		}

		public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object bean) {
			if (StringUtils.hasText(deactivationMethod)) {
				Method m = BeanUtils.resolveSignature(deactivationMethod, beanFactory.getType(beanName));
				try {
					m.invoke(bean, null);
				}
				catch (IllegalAccessException e) {
					throw new IllegalArgumentException(e);
				}
				catch (InvocationTargetException e) {
					log.error("Deactivation method on bean with name '" + beanName + "' threw an exception",
							e.getTargetException());
				}
			}
			else if (bean instanceof ServiceDeactivationLifecycleListener) {
				((ServiceDeactivationLifecycleListener) bean).deactivate(serviceRegistration.getClass());
			}
		}
	}


	/**
	 * @param contextClassloader The contextClassloader to set.
	 */
	public void setContextClassloader(int contextClassloader) {
		if (!CL_OPTIONS.getValues(null).contains(new Integer(contextClassloader)))
			throw new IllegalArgumentException("illegal constant:" + contextClassloader);

		this.contextClassloader = contextClassloader;
	}
	
	public void setContextClassloader(String options) {
		// transform "-" into "_" (for service-provider)
		if (options == null)
			throw new IllegalArgumentException("non-null argument required");

		this.contextClassloader = CL_OPTIONS.asNumber(options.replace("-", "_")).intValue();
	}

}
