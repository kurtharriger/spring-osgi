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
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.springframework.osgi.web.extender.WarDeployer;
import org.springframework.osgi.web.extender.internal.jetty.JettyWarDeployer;
import org.springframework.scheduling.timer.TimerTaskExecutor;

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
						log.trace("processing " + OsgiStringUtils.nullSafeToString(event) + " event for bundle "
								+ OsgiStringUtils.nullSafeNameAndSymName(bundle));

					maybeDeployWar(bundle);
					break;
				}
				case BundleEvent.STOPPING: {
					if (trace)
						log.trace("processing " + OsgiStringUtils.nullSafeToString(event) + " event for bundle "
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


		public DeployTask(Bundle bundle) {
			this.bundle = bundle;
		}

		public void run() {
			if (log.isDebugEnabled())
				log.debug("deploying bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle));
			managedWARs.put(bundle, new Date());
			warDeployer.deploy(bundle);

			if (log.isDebugEnabled())
				log.debug("bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "successfully deployed");
		}
	}

	/** undeploy war task */
	private class UndeployTask implements Runnable {

		/** bundle to deploy */
		private final Bundle bundle;


		public UndeployTask(Bundle bundle) {
			this.bundle = bundle;
		}

		public void run() {
			if (log.isDebugEnabled())
				log.debug("undeploying bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle));
			managedWARs.remove(bundle);
			warDeployer.undeploy(bundle);
			if (log.isDebugEnabled())
				log.debug("bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "successfully undeployed");
		}
	}

	/**
	 * Simple WAR deployment manager. Handles the IO process involved in
	 * deploying and undeploying the war.
	 * 
	 */
	private class DeploymentManager implements DisposableBean {

		/** queue lock */
		private final Object queueLock = new Object();

		// processing queue
		// TODO: replace this with a JDK5/backport concurrent queue
		// we actually need a map since we're using the same queue for bundles that get deployed, undeployed
		// as we process them in the arrival order. It also makes it simple to eliminate deploy/undeploy events
		// that haven't been processed yet

		// TODO: can replace this with a concurrent map (if order is not important)
		/**
		 * queue of bundles to deploy/undeploy (since the process takes some
		 * time)
		 */
		private final Map deploymentQueue = new LinkedHashMap();

		// the timer class is directly used since we need access to the TimerTask
		/** thread for deploying/undeploying bundles */
		private TimerTaskExecutor executor = new TimerTaskExecutor();


		public DeploymentManager() {
			executor.afterPropertiesSet();
		}

		public void deployBundle(Bundle bundle) {
			executor.execute(new DeployTask(bundle));
		}

		public void undeployBundle(Bundle bundle) {
			executor.execute(new UndeployTask(bundle));
		}

		public void destroy() throws Exception {
			executor.destroy();
			deploymentQueue.clear();
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
	 * Monitor used for dealing with the bundle activator and synchronous bundle
	 * threads
	 */
	private transient final Object monitor = new Object();

	/**
	 * Concurrent map of managed OSGi bundles considered wars. The keys are the
	 * bundle ID while the values actual bundle references.
	 */
	protected final ConcurrentMap managedWARs;

	/**
	 * Bundle listener for WARs.
	 */
	private SynchronousBundleListener warListener;

	/** war scanner */
	private WarScanner warScanner;

	/** war deployer */
	private WarDeployer warDeployer;

	private DeploymentManager deploymentManager;


	/**
	 * Constructs a new <code>WarLoaderListener</code> instance.
	 * 
	 */
	public WarLoaderListener() {
		this.managedWARs = CollectionFactory.createConcurrentMap(16);
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
		warDeployer = new JettyWarDeployer(bundleContext);

		// register war listener
		warListener = new WarBundleListener();

		bundleContext.addBundleListener(warListener);

		// check existing bundles
		Bundle[] bnds = bundleContext.getBundles();

		for (int i = 0; i < bnds.length; i++) {
			Bundle bundle = bnds[i];
			if (OsgiBundleUtils.isBundleActive(bundle)) {
				if (trace)
					log.trace("checking if bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + " is a war..");
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
			if (debug)
				log.debug(OsgiStringUtils.nullSafeNameAndSymName(bundle)
						+ " is a WAR, scheduling war deployment ... (detected web.xml at " + webXml + ")");

			deploymentManager.deployBundle(bundle);
		}
	}

	private void maybeUndeployWar(Bundle bundle) {
		boolean debug = log.isDebugEnabled();

		// TODO: use a separate thread
		Object value = managedWARs.get(bundle);

		// do a fast look-up to see if the bundle has already been deployed
		// if it has, then undeploy it
		if (value != null) {
			if (debug)
				log.debug(OsgiStringUtils.nullSafeNameAndSymName(bundle) + " is a WAR, scheduling war undeployment ...");

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
	}
}
