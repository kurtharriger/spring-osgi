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

package org.springframework.osgi.web.extender.internal.activator;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.CollectionFactory;
import org.springframework.core.ConcurrentMap;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.web.deployer.ContextPathStrategy;
import org.springframework.osgi.web.deployer.OsgiWarDeploymentException;
import org.springframework.osgi.web.deployer.WarDeployer;
import org.springframework.osgi.web.deployer.WarDeployment;
import org.springframework.osgi.web.extender.internal.scanner.WarScanner;
import org.springframework.scheduling.timer.TimerTaskExecutor;
import org.springframework.util.Assert;

/**
 * OSGi specific listener that bootstraps web applications packed as WARs (Web
 * ARchives). Additionally, it makes the BundleContext available in the
 * ServletContext so that various components can look it up.
 * 
 * @author Costin Leau
 * 
 */
public class WarLoaderListener implements BundleActivator {

	/**
	 * Bundle listener monitoring war-bundles that are being started/stopped.
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class WarBundleListener implements SynchronousBundleListener {

		public void bundleChanged(BundleEvent event) {
			Bundle bundle = event.getBundle();
			boolean trace = log.isTraceEnabled();

			// ignore current bundle for war deployment
			if (bundle.getBundleId() == bundleId) {
				return;
			}
			switch (event.getType()) {
				case BundleEvent.STARTED: {
					if (trace)
						log.trace("Processing " + OsgiStringUtils.nullSafeToString(event) + " event for bundle "
								+ OsgiStringUtils.nullSafeNameAndSymName(bundle));

					maybeDeployWar(bundle);
					break;
				}
				case BundleEvent.STOPPING: {
					if (trace)
						log.trace("Processing " + OsgiStringUtils.nullSafeToString(event) + " event for bundle "
								+ OsgiStringUtils.nullSafeNameAndSymName(bundle));

					maybeUndeployWar(bundle);
					break;
				}
				default:
					break;
			}
		}
	}

	/**
	 * Simple WAR deployment manager. Handles the IO process involved in
	 * deploying and undeploying the war.
	 */
	// TODO: in the future, this could be externalized so smarter implementations can be plugged in 
	private class DeploymentManager implements DisposableBean {

		/** association map between a bundle and its web deployment object */
		private final ConcurrentMap bundlesToDeployments = CollectionFactory.createConcurrentMap(16);

		/** thread for deploying/undeploying bundles */
		private TimerTaskExecutor executor = new TimerTaskExecutor();


		public DeploymentManager() {
			executor.afterPropertiesSet();
		}

		public void deployBundle(Bundle bundle, String contextPath) {
			executor.execute(new DeployTask(bundle, contextPath));
		}

		public void undeployBundle(Bundle bundle) {
			executor.execute(new UndeployTask(bundle));
		}

		public void destroy() throws Exception {
			executor.destroy();
		}


		/** deploy war task */
		private class DeployTask implements Runnable {

			/** bundle to deploy */
			private final Bundle bundle;

			private final String contextPath;

			private final String bundleName;


			public DeployTask(Bundle bundle, String contextPath) {
				this.bundle = bundle;
				this.contextPath = contextPath;
				this.bundleName = OsgiStringUtils.nullSafeNameAndSymName(bundle);
			}

			public void run() {
				try {
					WarDeployment deployment = warDeployer.deploy(bundle, contextPath);
					// deploy the bundle 
					bundlesToDeployments.put(bundle, deployment);
				}
				catch (OsgiWarDeploymentException ex) {
					// log exception
					log.error("War deployment of bundle " + bundleName + " failed", ex);
				}
			}
		}

		/** undeploy war task */
		private class UndeployTask implements Runnable {

			/** bundle to deploy */
			private final Bundle bundle;

			private final String bundleName;


			public UndeployTask(Bundle bundle) {
				this.bundle = bundle;
				this.bundleName = OsgiStringUtils.nullSafeNameAndSymName(bundle);
			}

			public void run() {
				WarDeployment deployment = (WarDeployment) bundlesToDeployments.remove(bundle);
				// double check that we do have a deployment
				if (deployment != null)
					try {
						deployment.undeploy();
					}
					catch (OsgiWarDeploymentException ex) {
						// log exception
						log.error("War undeployment of bundle " + bundleName + " failed", ex);
					}
			}
		}
	}


	/** logger */
	private static final Log log = LogFactory.getLog(WarLoaderListener.class);

	/** OSGi bundle context */
	private BundleContext bundleContext;

	/** Extender version */
	private Version extenderVersion;

	/** extender bundle id */
	private long bundleId;

	/** map used for tracking bundles deployed as wars */
	private final ConcurrentMap managedBundles;

	/**
	 * Bundle listener for WARs.
	 */
	private SynchronousBundleListener warListener;

	/** war scanner */
	private WarScanner warScanner;

	/** war deployer */
	private WarDeployer warDeployer;

	/** contextPath strategy */
	private ContextPathStrategy contextPathStrategy;

	private DeploymentManager deploymentManager;

	private WarListenerConfiguration configuration;


	/**
	 * Constructs a new <code>WarLoaderListener</code> instance.
	 * 
	 */
	public WarLoaderListener() {
		this.managedBundles = CollectionFactory.createConcurrentMap(16);
		deploymentManager = new DeploymentManager();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Bootstrapping procedure. Monitors deployed bundles and will scan for WARs
	 * known locations. Once such a war is detected, the web application will be
	 * deployed through a specific web deployer.
	 * 
	 */
	public void start(BundleContext context) throws Exception {
		this.bundleContext = context;
		this.bundleId = bundleContext.getBundle().getBundleId();
		this.extenderVersion = OsgiBundleUtils.getBundleVersion(context.getBundle());

		boolean trace = log.isTraceEnabled();

		log.info("Starting [" + bundleContext.getBundle().getSymbolicName() + "] bundle v.[" + extenderVersion + "]");

		// read configuration
		configuration = new WarListenerConfiguration(context);

		// instantiate fields
		warScanner = configuration.getWarScanner();
		warDeployer = configuration.getWarDeployer();
		contextPathStrategy = configuration.getContextPathStrategy();

		// register war listener
		warListener = new WarBundleListener();

		bundleContext.addBundleListener(warListener);

		// check existing bundles
		Bundle[] bnds = bundleContext.getBundles();

		for (int i = 0; i < bnds.length; i++) {
			Bundle bundle = bnds[i];
			if (OsgiBundleUtils.isBundleActive(bundle)) {
				if (trace)
					log.trace("Checking if bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + " is a war..");
				maybeDeployWar(bundle);
			}
		}
	}

	/**
	 * Checks if the given bundle is a war - if it is, deploy it.
	 * 
	 * @param bundle
	 */
	private void maybeDeployWar(Bundle bundle) {
		// exclude special bundles (such as the framework or this bundle)
		if (OsgiBundleUtils.isSystemBundle(bundle) || bundle.getBundleId() == bundleId)
			return;

		// check if the bundle is a war
		URL webXml = warScanner.getWebXmlConfiguration(bundle);

		boolean debug = log.isDebugEnabled();

		if (webXml != null) {
			// get bundle name
			String contextPath = contextPathStrategy.getContextPath(bundle);
			// make sure it doesn't contain spaces (causes subtle problems with Tomcat Jasper)
			Assert.doesNotContain(contextPath, " ", "context path should not contain whitespaces");
			log.info(OsgiStringUtils.nullSafeNameAndSymName(bundle)
					+ " is a WAR, scheduling war deployment on context path + [" + contextPath
					+ "] (detected web.xml at " + webXml + ")");

			// mark the bundle as managed
			managedBundles.put(bundle, contextPath);
			deploymentManager.deployBundle(bundle, contextPath);
		}
	}

	private void maybeUndeployWar(Bundle bundle) {
		boolean debug = log.isDebugEnabled();

		// do a fast look-up to see if the bundle has already been deployed
		// if it has, then undeploy it
		String contextPath = (String) managedBundles.remove(bundle);
		if (contextPath != null) {
			log.info(OsgiStringUtils.nullSafeNameAndSymName(bundle)
					+ " is a WAR, scheduling war undeployment with context path [" + contextPath + "]");

			deploymentManager.undeployBundle(bundle);
		}
	}

	public void stop(BundleContext context) throws Exception {

		// unregister listener
		if (warListener != null) {
			bundleContext.removeBundleListener(warListener);
			warListener = null;
		}

		// destroy any tasks that have to be processed
		deploymentManager.destroy();

		configuration.destroy();
	}
}
