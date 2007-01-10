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
import org.osgi.framework.BundleEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.osgi.context.support.ApplicationContextConfiguration;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.context.support.LocalBundleContext;
import org.springframework.osgi.context.support.NamespacePlugins;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContextFactory;
import org.springframework.osgi.context.support.OsgiResourceUtils;
import org.springframework.osgi.context.support.SpringBundleEvent;
import org.springframework.util.Assert;
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
	private final ApplicationEventMulticaster mcast;
	private Throwable creationTrace;

	/**
	 * Find spring resources in the given bundle, and if an application context needs to
	 * be created, create it and add it to the map, keyed by bundle id
	 *
	 * @param forBundle
	 * @param applicationContextMap
	 */
	public ApplicationContextCreator(
		Bundle forBundle,
		Map applicationContextMap,
		Map contextsPendingInitializationMap,
		OsgiBundleXmlApplicationContextFactory contextFactory,
		NamespacePlugins namespacePlugins,
		ApplicationEventMulticaster mcast) {
		this.bundle = forBundle;
		this.applicationContextMap = applicationContextMap;
		this.contextsPendingInitializationMap = contextsPendingInitializationMap;
		this.contextFactory = contextFactory;
		this.namespacePlugins = namespacePlugins;
		this.mcast = mcast;
		// Do some sanity checking.
		Assert.notNull(mcast);
		Long bundleKey = Long.valueOf(this.bundle.getBundleId());
		synchronized (this.applicationContextMap) {
			synchronized (this.contextsPendingInitializationMap) {
				Assert.isTrue(!contextsPendingInitializationMap.containsKey(bundleKey), "Duplicate context created!");
				Assert.isTrue(!applicationContextMap.containsKey(bundleKey), "Duplicate context created!");
			}
		}

		if (log.isInfoEnabled()) {
			creationTrace = new Throwable().fillInStackTrace();
		}
	}

	/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
	public void run() {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		ConfigurableApplicationContext applicationContext = null;
		Long bundleKey = Long.valueOf(this.bundle.getBundleId());

		ApplicationContextConfiguration config = new ApplicationContextConfiguration(bundle);
		if (!config.isSpringPoweredBundle()) {
			return;
		}

		postEvent(BundleEvent.STARTING);

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

			ClassLoader cl = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle);
			Thread.currentThread().setContextClassLoader(cl);
			LocalBundleContext.setContext(bundleContext);

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
				this.contextsPendingInitializationMap.put(bundleKey, applicationContext);
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
			postEvent(BundleEvent.STARTED);
		}
		catch (Throwable t) {
			if (log.isErrorEnabled()) {
				log.error("Unable to create application context for [" +
					bundle.getSymbolicName() + "]", t);
				if (log.isInfoEnabled()) {
					log.info("Calling code: ", creationTrace);
				}
			}
			// do not change locking order without also changing application context closer
			synchronized (this.applicationContextMap) {
				synchronized (this.contextsPendingInitializationMap) {
					this.contextsPendingInitializationMap.remove(bundleKey);
					this.applicationContextMap.remove(bundleKey);
				}
			}
			postEvent(BundleEvent.STOPPED);
		}
		finally {
			Thread.currentThread().setContextClassLoader(ccl);
		}
	}

	private void postEvent(int starting) {
		mcast.multicastEvent(new SpringBundleEvent(starting, bundle));
	}
}
