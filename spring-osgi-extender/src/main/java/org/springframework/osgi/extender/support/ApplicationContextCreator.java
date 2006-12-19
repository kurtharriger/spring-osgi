/*
 * Copyright 2006 the original author or authors.
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
package org.springframework.osgi.extender.support;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContextFactory;
import org.springframework.osgi.context.support.OsgiResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Search a bundle for Spring resources, and if found create an application context for it.
 * 
 * @author acolyer
 */
public class ApplicationContextCreator implements Runnable {

	private static final Log log = LogFactory.getLog(ApplicationContextCreator.class);

	private final Bundle bundle;
	private final Map applicationContextMap;
	private final Map contextsPendingInitializationMap;
	private final OsgiBundleXmlApplicationContextFactory contextFactory;
	private final NamespacePlugins namespacePlugins;
	
	/**
	 * Find spring resources in the given bundle, and if an application context needs to
	 * be created, create it and add it to the map, keyed by bundle id
	 * @param forBundle
	 * @param applicationContextMap
	 */
	public ApplicationContextCreator(
			Bundle forBundle, 
			Map applicationContextMap,
			Map contextsPendingInitializationMap,
			OsgiBundleXmlApplicationContextFactory contextFactory,
			NamespacePlugins namespacePlugins) {
		this.bundle = forBundle;
		this.applicationContextMap = applicationContextMap;
		this.contextsPendingInitializationMap = contextsPendingInitializationMap;
		this.contextFactory = contextFactory;
		this.namespacePlugins = namespacePlugins;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		ConfigurableApplicationContext applicationContext = null;
		Long bundleKey = Long.valueOf(this.bundle.getBundleId());
		
		ApplicationContextConfiguration config = new ApplicationContextConfiguration(bundle);
		if (!config.isSpringPoweredBundle()) {
			return;
		}

		BundleContext bundleContext = OsgiResourceUtils.getBundleContext(bundle);
		if (bundleContext == null) {
			log.error("Could not resolve BundleContext for bundle [" + bundle + "]");
			return;
		}
		
		try {
			if (log.isInfoEnabled()) {
				log.info("Starting bundle [" + bundle.getSymbolicName() 
						+ "] with configuration ["
						+ StringUtils.arrayToCommaDelimitedString(config.getConfigurationLocations()) + "]");
			}
			
			// create app context, the beans are not yet created at this point
			applicationContext = this.contextFactory.createApplicationContextWithBundleContext(
					bundleContext,
					config.getConfigurationLocations(), 
					this.namespacePlugins, 
					config.waitForDependencies());

			synchronized (this.contextsPendingInitializationMap) {
				// creating the beans may take a long time (possible 'forever') if the
				// service dependencies are not satisfied. We need to be able to 
				// stop this bundle and stop the context creation even before it is fully
				// completed initializing.
				this.contextsPendingInitializationMap.put(bundleKey,applicationContext);
			}
			
			applicationContext.refresh();
			
			// ensure no-one else modifies the context map while we do this
			// do not change locking order without also changing ApplicationContextCloser
			synchronized (this.applicationContextMap) {
				synchronized (this.contextsPendingInitializationMap) {
					if (this.contextsPendingInitializationMap.containsKey(bundleKey)) {
						// it is possible the key is no longer in the map if the bundle was 
						// stopped during the time it took us to get here...
						this.contextsPendingInitializationMap.remove(bundleKey);
						this.applicationContextMap.put(bundleKey, applicationContext);
					}
				}
			}

		}
		catch (Throwable t) {
			if (log.isErrorEnabled()) {
				log.error("Unable to create application context for [" + 
					bundle.getSymbolicName() + "]",t);
			}
			// do not change locking order without also changing application context closer
			synchronized (this.applicationContextMap) {
				synchronized (this.contextsPendingInitializationMap) {
					this.contextsPendingInitializationMap.remove(bundleKey);
					this.applicationContextMap.remove(bundleKey);
				}
			}
		}
	}

}
