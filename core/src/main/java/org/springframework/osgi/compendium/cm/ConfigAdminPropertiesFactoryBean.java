/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.compendium.cm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.compendium.internal.cm.CMUtils;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.service.exporter.support.ServicePropertiesChangeEvent;
import org.springframework.osgi.service.exporter.support.ServicePropertiesChangeListener;
import org.springframework.osgi.service.exporter.support.ServicePropertiesListenerManager;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.osgi.util.internal.MapBasedDictionary;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * FactoryBean returning the properties stored under a given persistent id in
 * the ConfigurationAdmin service. Once retrieved, the properties will remain
 * the same, even when the configuration object that it maps, changes.
 * 
 * <b>Note:</b> This implementation performs a lazy initialization of the
 * properties to receive the most up to date configuration.
 * 
 * @author Costin Leau
 * @see Configuration
 * @see ConfigurationAdmin
 * @see org.springframework.core.io.support.PropertiesFactoryBean
 */
public class ConfigAdminPropertiesFactoryBean implements BundleContextAware, InitializingBean, DisposableBean,
		FactoryBean<Properties> {

	@SuppressWarnings("unchecked")
	private class ConfigurationWatcher implements ManagedService {

		public void updated(Dictionary props) throws ConfigurationException {
			if (log.isTraceEnabled())
				log.trace("Configuration [" + persistentId + "] has been updated with properties " + props);

			// update properties
			initProperties(properties, new MapBasedDictionary(props));
			// inform listeners
			((ChangeableProperties) properties).notifyListeners();
		}
	}

	private class ChangeableProperties extends Properties implements ServicePropertiesListenerManager {

		private List<ServicePropertiesChangeListener> listeners = Collections.synchronizedList(new ArrayList<ServicePropertiesChangeListener>(
			4));


		public void addListener(ServicePropertiesChangeListener listener) {
			if (listener != null) {
				listeners.add(listener);
			}
		}

		public void removeListener(ServicePropertiesChangeListener listener) {
			if (listener != null) {
				listeners.remove(listener);
			}
		}

		void notifyListeners() {
			ServicePropertiesChangeEvent event = new ServicePropertiesChangeEvent(this);
			synchronized (listeners) {
				for (Iterator<ServicePropertiesChangeListener> iterator = listeners.iterator(); iterator.hasNext();) {
					ServicePropertiesChangeListener listener = iterator.next();
					listener.propertiesChange(event);
				}
			}
		}
	}


	/** logger */
	private static final Log log = LogFactory.getLog(ConfigAdminPropertiesFactoryBean.class);

	private volatile String persistentId;
	private volatile Properties properties;
	private BundleContext bundleContext;
	private boolean localOverride = false;
	private Properties localProperties;
	private volatile boolean dynamic = false;
	private volatile ServiceRegistration registration;


	public void afterPropertiesSet() throws Exception {
		Assert.hasText(persistentId, "persistentId property is required");
		Assert.notNull(bundleContext, "bundleContext property is required");

		if (dynamic) {
			// create special Properties object
			properties = new ChangeableProperties();
			// init properties
			// copy config admin properties
			try {
				initProperties(properties, CMUtils.getConfiguration(bundleContext, persistentId));
			}
			catch (IOException ioe) {
				throw new BeanInitializationException("Cannot retrieve configuration for pid=" + persistentId, ioe);
			}

			// perform eager registration
			registration = CMUtils.registerManagedService(bundleContext, new ConfigurationWatcher(), persistentId);
		}
	}

	public void destroy() throws Exception {
		OsgiServiceUtils.unregisterService(registration);
		registration = null;
	}

	/**
	 * Reads the current properties in the ConfigurationAdmin.
	 * 
	 * @return
	 */
	private Properties initProperties(Properties target, Map<?, ?> cmConfig) {

		synchronized (target) {
			target.clear();

			// merge the local properties (upfront)
			if (localProperties != null && !localOverride) {
				CollectionUtils.mergePropertiesIntoMap(localProperties, target);
			}

			target.putAll(cmConfig);

			// merge local properties (if needed)
			if (localProperties != null && localOverride) {
				CollectionUtils.mergePropertiesIntoMap(localProperties, target);
			}

			return target;
		}
	}

	public Properties getObject() throws Exception {
		// if static, perform lazy initialization
		if (properties == null) {
			try {
				properties = initProperties(new Properties(), CMUtils.getConfiguration(bundleContext, persistentId));
			}
			catch (IOException ioe) {
				throw new BeanInitializationException("Cannot retrieve configuration for pid=" + persistentId, ioe);
			}
		}

		return properties;
	}

	public Class<? extends Properties> getObjectType() {
		return (dynamic ? ChangeableProperties.class : Properties.class);
	}

	public boolean isSingleton() {
		return true;
	}

	/**
	 * Returns the persistentId.
	 * 
	 * @return Returns the persistentId
	 */
	public String getPersistentId() {
		return persistentId;
	}

	/**
	 * Sets the ConfigurationAdmin persistent Id that the bean should read.
	 * 
	 * @param persistentId The persistentId to set.
	 */
	public void setPersistentId(String persistentId) {
		this.persistentId = persistentId;
	}

	/**
	 * Sets the local properties, e.g. via the nested tag in XML bean
	 * definitions. These can be considered defaults, to be overridden by
	 * properties loaded from the Configuration Admin.
	 */
	public void setProperties(Properties properties) {
		this.localProperties = properties;
	}

	/**
	 * Sets whether local properties override properties from files.
	 * <p>
	 * Default is "false": Properties from the Configuration Admin override
	 * local defaults. Can be switched to "true" to let local properties
	 * override the Configuration Admin properties.
	 */
	public void setLocalOverride(boolean localOverride) {
		this.localOverride = localOverride;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	/**
	 * Indicates whether the returned properties object is dynamic or not.
	 * 
	 * @return boolean indicating if the configuration object is dynamic
	 */
	public boolean isDynamic() {
		return dynamic;
	}

	/**
	 * Indicates if the returned configuration is dynamic or static. A static
	 * configuration (default) ignores any updates made to the configuration
	 * admin entry that it maps. A dynamic configuration on the other hand will
	 * reflect the changes in its content. Third parties can be notified through
	 * the {@link ServicePropertiesChangeListener} contract.
	 * 
	 * @param dynamic whether the returned object reflects the changes in the
	 *        configuration admin or not.
	 */
	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}
}