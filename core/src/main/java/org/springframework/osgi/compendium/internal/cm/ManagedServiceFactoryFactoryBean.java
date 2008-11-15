/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.compendium.internal.cm;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.service.exporter.OsgiServiceRegistrationListener;
import org.springframework.osgi.service.exporter.support.AutoExport;
import org.springframework.osgi.service.exporter.support.ExportContextClassLoader;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.osgi.util.internal.MapBasedDictionary;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link FactoryBean Factory} class that automatically manages instances based
 * on the configuration available inside a {@link ManagedServiceFactory}.
 * 
 * The factory returns a list of {@link ServiceRegistration} of all published
 * instances.
 * 
 * @author Costin Leau
 */
public class ManagedServiceFactoryFactoryBean implements InitializingBean, BeanClassLoaderAware, BeanFactoryAware,
		BundleContextAware, DisposableBean, FactoryBean {

	/**
	 * Configuration Admin whiteboard 'listener'.
	 * 
	 * @author Costin Leau
	 */
	private class ConfigurationWatcher implements ManagedServiceFactory {

		public void deleted(String pid) {
			if (log.isTraceEnabled())
				log.trace("Configuration [" + pid + "] has been deleted");
			destroyInstance(pid);
		}

		public String getName() {
			return "Spring DM managed-service-factory support";
		}

		public void updated(String pid, Dictionary props) throws ConfigurationException {
			if (log.isTraceEnabled())
				log.trace("Configuration [" + pid + "] has been updated with properties " + props);
			createOrUpdate(pid, props);
		}

	}


	/** logger */
	private static final Log log = LogFactory.getLog(ManagedServiceFactoryFactoryBean.class);

	/** visibility monitor */
	private final Object monitor = new Object();
	/** Configuration Admin fpid */
	private String factoryPid;
	/** bundle context */
	private BundleContext bundleContext;
	/** embedded bean factory for instance management */
	private ConfigurableBeanFactory beanFactory;
	/** the bean factory seen as a bdr */
	private BeanDefinitionRegistry definitionRegistry;
	/** bean definition template */
	private BeanDefinition templateDefinition;
	/** owning bean factory - can be null */
	private BeanFactory owningBeanFactory;
	/** configuration watcher registration */
	private ServiceRegistration configurationWatcher;
	/** instance registrations */
	private Map registeredServices = CollectionFactory.createConcurrentMap(8);
	/** update callback */
	private UpdateCallback updateCallback;

	// exporting template
	/** listeners */
	private OsgiServiceRegistrationListener[] listeners = new OsgiServiceRegistrationListener[0];
	/** auto export */
	private AutoExport autoExport = AutoExport.DISABLED;
	/** ccl */
	private ExportContextClassLoader ccl = ExportContextClassLoader.UNMANAGED;
	/** interfaces */
	private Class[] interfaces;
	/** class loader */
	private ClassLoader classLoader;

	// update configuration
	private UpdateStrategy updateStrategy;
	private String updateMethod;

	/**
	 * destroyed flag - used since some CM implementations still call the
	 * service even though it was unregistered
	 */
	private boolean destroyed = false;


	public void afterPropertiesSet() throws Exception {

		synchronized (monitor) {
			Assert.notNull(factoryPid, "factoryPid required");
			Assert.notNull(bundleContext, "bundleContext is required");
			Assert.notNull(templateDefinition, "templateDefinition is required");

			Assert.isTrue(!AutoExport.DISABLED.equals(autoExport) || !ObjectUtils.isEmpty(interfaces),
				"No service interface(s) specified and auto-export discovery disabled; change at least one of these properties");
		}

		// make sure the scope is singleton
		templateDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
		createEmbeddedBeanFactory();

		updateCallback = CMUtils.createCallback(updateStrategy, updateMethod, beanFactory);

		registerService();
	}

	public void destroy() throws Exception {
		synchronized (monitor) {
			destroyed = true;
			// remove factory service
			OsgiServiceUtils.unregisterService(configurationWatcher);
			configurationWatcher = null;
			// destroy instances
			destroyFactory();
		}
	}

	private void createEmbeddedBeanFactory() {
		synchronized (monitor) {
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory(owningBeanFactory);
			if (owningBeanFactory instanceof ConfigurableBeanFactory) {
				bf.copyConfigurationFrom((ConfigurableBeanFactory) owningBeanFactory);
			}
			// just to be on the safe side
			bf.setBeanClassLoader(classLoader);

			beanFactory = bf;
			definitionRegistry = bf;
		}
	}

	private void registerService() {
		synchronized (monitor) {
			Dictionary props = new Hashtable();
			props.put(Constants.SERVICE_PID, factoryPid);

			configurationWatcher = bundleContext.registerService(ManagedServiceFactory.class.getName(),
				new ConfigurationWatcher(), props);
		}
	}

	// no monitor since the calling method already holds it
	private void destroyFactory() {
		if (beanFactory != null) {
			beanFactory.destroySingletons();
			beanFactory = null;
			definitionRegistry = null;
		}
	}

	private void createOrUpdate(String pid, Dictionary props) {
		synchronized (monitor) {
			if (destroyed)
				return;

			if (beanFactory.containsBean(pid)) {
				updateInstance(pid, props);
			}
			else {
				createInstance(pid, props);
			}
		}
	}

	private void createInstance(String pid, Dictionary props) {
		synchronized (monitor) {
			if (destroyed)
				return;

			definitionRegistry.registerBeanDefinition(pid, templateDefinition);
			// create instance
			Object bean = beanFactory.getBean(pid);
			registerService(pid, bean);
		}
	}

	private void registerService(String beanName, Object bean) {
		// add properties
		Dictionary props = new Hashtable(3);
		props.put(Constants.SERVICE_PID, beanName);

		registeredServices.put(beanName, createExporter(beanName, bean));
	}

	private Object createExporter(String beanName, Object bean) {
		OsgiServiceFactoryBean exporter = new OsgiServiceFactoryBean();
		exporter.setAutoExport(autoExport);
		exporter.setBeanClassLoader(classLoader);
		exporter.setBeanName(beanName);
		exporter.setBundleContext(bundleContext);
		exporter.setContextClassLoader(ccl);
		exporter.setInterfaces(interfaces);
		exporter.setListeners(listeners);
		exporter.setTarget(bean);

		try {
			exporter.afterPropertiesSet();
		}
		catch (Exception ex) {
			throw new BeanCreationException("Cannot publish bean for pid " + beanName, ex);
		}
		return exporter;
	}

	private void updateInstance(String pid, Dictionary props) {
		if (updateCallback != null) {
			Object instance = beanFactory.getBean(pid);
			updateCallback.update(instance, new MapBasedDictionary(props));
		}
	}

	private void destroyInstance(String pid) {
		synchronized (monitor) {
			// bail out fast
			if (destroyed)
				return;

			unregisterService(pid);
			if (definitionRegistry.containsBeanDefinition(pid)) {
				// remove definition and instance
				definitionRegistry.removeBeanDefinition(pid);
			}
		}
	}

	private void unregisterService(String pid) {
		OsgiServiceFactoryBean exporterFactory = (OsgiServiceFactoryBean) registeredServices.remove(pid);

		if (exporterFactory != null) {

			if (log.isTraceEnabled()) {
				try {
					log.trace("Unpublishing bean for pid " + pid + " w/ registration " + exporterFactory.getObject());
				}
				catch (Exception ex) {
					// log the exception and continue
					log.error("Could not retrieve registration for pid " + pid, ex);
				}
			}
			exporterFactory.destroy();
		}
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		synchronized (monitor) {
			this.owningBeanFactory = beanFactory;
		}
	}

	public void setBundleContext(BundleContext bundleContext) {
		synchronized (monitor) {
			this.bundleContext = bundleContext;
		}
	}

	public Object getObject() throws Exception {
		return registeredServices.values();
	}

	public Class getObjectType() {
		return Collection.class;
	}

	public boolean isSingleton() {
		return false;
	}

	/**
	 * Sets the listeners interested in registration and unregistration events.
	 * 
	 * @param listeners registration/unregistration listeners.
	 */
	public void setListeners(OsgiServiceRegistrationListener[] listeners) {
		if (listeners != null)
			this.listeners = listeners;
	}

	/**
	 * @param factoryPid The factoryPid to set.
	 */
	public void setFactoryPid(String factoryPid) {
		synchronized (monitor) {
			this.factoryPid = factoryPid;
		}
	}

	/**
	 * @param templateDefinition The templateDefinition to set.
	 */
	public void setTemplateDefinition(BeanDefinition[] templateDefinition) {
		if (templateDefinition != null && templateDefinition.length > 0) {
			this.templateDefinition = templateDefinition[0];
		}
		else {
			this.templateDefinition = null;
		}
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * @param autoExport The autoExport to set.
	 */
	public void setAutoExport(AutoExport autoExport) {
		this.autoExport = autoExport;
	}

	/**
	 * @param ccl The ccl to set.
	 */
	public void setContextClassLoader(ExportContextClassLoader ccl) {
		this.ccl = ccl;
	}

	/**
	 * @param interfaces The interfaces to set.
	 */
	public void setInterfaces(Class[] interfaces) {
		this.interfaces = interfaces;
	}

	/**
	 * @param updateStrategy The updateStrategy to set.
	 */
	public void setUpdateStrategy(UpdateStrategy updateStrategy) {
		this.updateStrategy = updateStrategy;
	}

	/**
	 * @param updateMethod The updateMethod to set.
	 */
	public void setUpdateMethod(String updateMethod) {
		this.updateMethod = updateMethod;
	}
}