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

package org.springframework.osgi.web.extender.internal;

import java.net.URL;
import java.util.Date;
import java.util.Timer;

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
import org.springframework.osgi.web.extender.ContextPathStrategy;
import org.springframework.osgi.web.extender.WarDeployer;
import org.springframework.osgi.web.extender.internal.tomcat.TomcatWarDeployer;
import org.springframework.scheduling.timer.TimerTaskExecutor;
import org.springframework.util.Assert;

/**
 * OSGi specific listener that bootstraps web applications packed as WARs (Web
 * ARchives).
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
			boolean debug = log.isDebugEnabled();
			if (debug)
				log.debug("Deploying bundle " + bundleName);
			managedBundles.put(bundle, new Date());
			try {
				warDeployer.deploy(bundle, contextPath);

				if (debug)
					log.debug("Bundle " + bundleName + "successfully deployed");

			}
			catch (Exception ex) {
				// log exception
				log.error("War deployment of bundle " + bundleName + " failed", ex);
			}
		}
	}

	/** undeploy war task */
	private class UndeployTask implements Runnable {

		/** bundle to deploy */
		private final Bundle bundle;

		private final String contextPath;

		private final String bundleName;


		public UndeployTask(Bundle bundle, String contextPath) {
			this.bundle = bundle;
			this.contextPath = contextPath;
			this.bundleName = OsgiStringUtils.nullSafeNameAndSymName(bundle);
		}

		public void run() {
			boolean debug = log.isDebugEnabled();

			if (debug)
				log.debug("Undeploying bundle " + bundleName);
			managedBundles.remove(bundle);
			try {
				warDeployer.undeploy(bundle, contextPath);
				if (debug)
					log.debug("Bundle " + bundleName + "successfully undeployed");
			}
			catch (Exception ex) {
				// log exception
				log.error("War undeployment of bundle " + bundleName + " failed", ex);
			}
		}
	}

	/**
	 * Simple WAR deployment manager. Handles the IO process involved in
	 * deploying and undeploying the war.
	 */
	// TODO: make this pluggable so that smarter managers can be used
	private class DeploymentManager implements DisposableBean {

		// the timer class is directly used since we need access to the TimerTask
		/** thread for deploying/undeploying bundles */
		private TimerTaskExecutor executor = new TimerTaskExecutor() {

			protected Timer createTimer() {
				return new Timer("Spring OSGi War Deployer", true);
			}
		};


		public DeploymentManager() {
			executor.afterPropertiesSet();
		}

		public void deployBundle(Bundle bundle, String contextPath) {
			executor.execute(new DeployTask(bundle, contextPath));
		}

		public void undeployBundle(Bundle bundle, String contextPath) {
			executor.execute(new UndeployTask(bundle, contextPath));
		}

		public void destroy() throws Exception {
			executor.destroy();
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

	/**
	 * Concurrent map of managed OSGi bundles considered wars. The keys are the
	 * bundle ID while the values actual bundle references.
	 */
	protected final ConcurrentMap managedBundles;

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

		log.info("Starting org.springframework.osgi.web.extender bundle v.[" + extenderVersion + "]");

		// instantiate fields
		// TODO: make this configurable through an XML file
		warScanner = new DefaultWarScanner();

		// TODO: make war deployer plug-able
		warDeployer = new TomcatWarDeployer(bundleContext);

		// TODO: make context path plug-able
		contextPathStrategy = new DefaultContextPathStrategy();

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
			if (debug)
				log.debug(OsgiStringUtils.nullSafeNameAndSymName(bundle)
						+ " is a WAR, scheduling war deployment on context path + [" + contextPath
						+ "] (detected web.xml at " + webXml + ")");

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
			if (debug)
				log.debug(OsgiStringUtils.nullSafeNameAndSymName(bundle)
						+ " is a WAR, scheduling war undeployment with context path [" + contextPath + "]");

			deploymentManager.undeployBundle(bundle, contextPath);
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

		// TODO: should we undeploy as well (?)
	}
}
