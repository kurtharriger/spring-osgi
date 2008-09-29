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

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.util.internal.MapBasedDictionary;

/**
 * Class responsible for interacting with the Configuration Admin service. It
 * handles the retrieval and updates for a given persistent id.
 * 
 * @author Costin Leau
 * @see org.osgi.service.cm.ConfigurationAdmin
 * @see ManagedService
 */
class ConfigurationAdminManager {

	/**
	 * Configuration Admin whiteboard 'listener'.
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class ConfigurationWatcher implements ManagedService {

		public void updated(Dictionary props) throws ConfigurationException {
			synchronized (monitor) {
				// update properties
				properties = new MapBasedDictionary(props);
				// invoke callback
				if (beanManager != null)
					beanManager.updated(properties);
			}
		}
	}


	private final BundleContext bundleContext;
	private final Object pid;
	// up to date configuration
	private Map properties = null;
	private boolean initialized = false;
	private ManagedServiceBeanManager beanManager;
	private final Object monitor = new Object();


	/**
	 * Constructs a new <code>ConfigurationAdminManager</code> instance.
	 * 
	 */
	public ConfigurationAdminManager(Object pid, BundleContext bundleContext) {
		this.pid = pid;
		this.bundleContext = bundleContext;
	}

	public void setBeanManager(ManagedServiceBeanManager beanManager) {
		synchronized (monitor) {
			this.beanManager = beanManager;
		}
	}

	/**
	 * Returns the configuration 'monitored' by this managed.
	 * 
	 * @return monitored configuration
	 */
	public Map getConfiguration() {
		initialize();
		synchronized (monitor) {
			return properties;
		}
	}

	/**
	 * Initializes the conversation with the configuration admin. This method
	 * allows for lazy service registration to avoid notification being sent w/o
	 * any beans requesting it.
	 */
	private void initialize() {
		synchronized (monitor) {
			if (initialized)
				return;
			initialized = true;
		}

		Properties props = new Properties();
		props.put(Constants.SERVICE_PID, pid);
		Bundle bundle = bundleContext.getBundle();
		props.put(Constants.BUNDLE_SYMBOLICNAME, OsgiStringUtils.nullSafeSymbolicName(bundle));
		props.put(Constants.BUNDLE_VERSION, OsgiBundleUtils.getBundleVersion(bundle));

		bundleContext.registerService(ManagedService.class.getName(), new ConfigurationWatcher(), props);
	}
}
