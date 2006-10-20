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
 */
package org.springframework.osgi.context;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.context.support.AbstractRefreshableOsgiBundleApplicationContext;
import org.springframework.osgi.context.support.DefaultOsgiBundleXmlApplicationContextFactory;
import org.springframework.osgi.context.support.NamespacePlugins;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContextFactory;
import org.springframework.osgi.context.support.OsgiPlatformDetector;
import org.springframework.osgi.context.support.OsgiResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Bootstrap listener for Spring applications inside an OSGi environment. The
 * listener looks in well known locations for Spring-OSGi specific information
 * and, based on it, publishes the application context inside OSGi.
 * 
 * @author Bill Gallagher
 * @author Andy Piper
 * @author Hal Hildebrand
 */
public class ContextLoaderListener implements BundleActivator, SynchronousBundleListener {
	// The standard for META-INF header keys excludes ".", so these constants
	// must use "-"
	private static final String CONTEXT_LOCATION_HEADER = "org-springframework-context";
	private static final String CONTEXT_LOCATION_DELIMITERS = ", ";
	private static final String SPRING_CONTEXT_DIRECTORY = "/META-INF/spring/";
	private static final String DEFAULT_GLOBAL_CONFIG = "spring.xml";
	private static final String GLOBAL_CONFIG_PROP = "org.springframework.osgi.config";
	private static final String SPRING_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";
	// TODO - HSH: Needs review. Hate using a static, but the
	// OsgiWebApplicationContext seems to require it
	private static final NamespacePlugins plugins = new NamespacePlugins();

	private static final Log log = LogFactory.getLog(ContextLoaderListener.class);

	private static class ContextEntry {
		public ContextEntry(String config) {
			this.config = new File(config);
		}

		private AbstractRefreshableOsgiBundleApplicationContext context;
		private long lastModified;
		private File config;
	}

	private ContextEntry[] contexts;
	private long bundleId;

	private OsgiBundleXmlApplicationContextFactory contextFactory = new DefaultOsgiBundleXmlApplicationContextFactory();

	private Map managedBundles = new HashMap();
	
	// required to work around knopflerfish getResource bug...
	private boolean isKnopflerfish = false;

	public static NamespacePlugins plugins() {
		return plugins;
	}

	// Required by the BundleActivator contract
	public ContextLoaderListener() {
		// load the 'watched' Spring XML file locations
		String config = System.getProperty(GLOBAL_CONFIG_PROP);
		if (config == null) {
			config = DEFAULT_GLOBAL_CONFIG;
		}
		String[] files = StringUtils.tokenizeToStringArray(config, ",");
		contexts = new ContextEntry[files.length];
		for (int i = 0; i < files.length; i++) {
			contexts[i] = new ContextEntry(files[i]);
		}
	}

	public void start(BundleContext context) throws Exception {
		this.isKnopflerfish = OsgiPlatformDetector.isKnopflerfish(context);
		
		// Collect all previously resolved bundles which have namespace plugins
		Bundle[] previousBundles = context.getBundles();
		for (int i = 0; i < previousBundles.length; i++) {
			resolveBundle(previousBundles[i]);
		}

		bundleId = context.getBundle().getBundleId();

		context.addBundleListener(this);

		ApplicationContext parent = null;
		for (int i = 0; i < contexts.length; i++) {
			contexts[i].lastModified = contexts[i].config.lastModified();
			if (contexts[i].config.exists()) {
				contexts[i].context = contextFactory.createApplicationContextWithBundleContext(parent, context,
						new String[] { contexts[i].config.toURL().toString() }, plugins, false);
				parent = contexts[i].context;
			}
		}

		// TODO: is this really necessary?  - the bundle is unpacked inside the OSGi framework
		// and the file isn't actually accessed. It's likely that in case of an update
		// the bundle will be stopped and then started again.
		Thread updateThread = new UpdateThread();
		updateThread.setDaemon(true);
		updateThread.start();
	}

	public void stop(BundleContext context) throws Exception {
		for (int i = 0; i < contexts.length; i++) {
			if (contexts[i].context != null) {
				contexts[i].context.close();
			}
		}
	}

	public void bundleChanged(BundleEvent event) {
		if (event.getBundle().getBundleId() == bundleId) {
			return;
		}

		switch (event.getType()) {
		case BundleEvent.STARTED:
			startBundle(event.getBundle());
			break;
		case BundleEvent.STOPPING:
			stopBundle(event.getBundle());
			break;
		case BundleEvent.RESOLVED:
			resolveBundle(event.getBundle());
			break;
		case BundleEvent.UNRESOLVED:
			unresolveBundle(event.getBundle());
			break;
		}
	}

	private void startBundle(Bundle bundle) {
		String[] applicationContextLocations = getApplicationContextLocations(bundle);

		BundleContext bundleContext = OsgiResourceUtils.getBundleContext(bundle);
		if (bundleContext == null) {
			log.error("Could not resolve BundleContext for bundle [" + bundle + "]");
			return;
		}
		// No need to set the CCL. OsgiBundleXmlApplicationContext will do this
		// for us.
		try {
			if (log.isInfoEnabled()) {
				log.info("Starting bundle [" + bundle.getHeaders().get(Constants.BUNDLE_NAME)
						+ "] with configuration ["
						+ (applicationContextLocations == null ? "none" : StringUtils.arrayToCommaDelimitedString(applicationContextLocations) + "]"));
			}
			ConfigurableApplicationContext ctx = contextFactory.createApplicationContextWithBundleContext(null,
					bundleContext, applicationContextLocations, plugins, true);
			managedBundles.put(bundle, ctx);
		}
		catch (Throwable thr) {
			thr.printStackTrace();
		}
	}

	private void stopBundle(Bundle bundle) {
		ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) managedBundles.remove(bundle);
		if (ctx != null) {
			ctx.close();
		}
	}

	private void resolveBundle(Bundle bundle) {
		if (isKnopflerfish) {
			// knopflerfish (2.0.0) has a bug which gives a classcast exception if you call getResource
			// from outside of the bundle, yet getResource works bettor on equinox....
			if (bundle.getEntry(SPRING_HANDLER_MAPPINGS_LOCATION) != null
					|| bundle.getEntry(PluggableSchemaResolver.DEFAULT_SCHEMA_MAPPINGS_LOCATION) != null) {
				plugins.addHandler(bundle);
			}			
		}
		else {
			if (bundle.getResource(SPRING_HANDLER_MAPPINGS_LOCATION) != null
					|| bundle.getResource(PluggableSchemaResolver.DEFAULT_SCHEMA_MAPPINGS_LOCATION) != null) {
				plugins.addHandler(bundle);
			}
		}
	}

	private void unresolveBundle(Bundle bundle) {
		plugins.removeHandler(bundle);
	}

	/**
	 * Retrieves the list of xml resources which compose the application
	 * context. <p/> The org.springframework.context manifest header attribute,
	 * if present, is parsed to create an ordered list of resource names in the
	 * spring context directory for creating the application context <p/> If the
	 * org.springframework.context header is not present, the entire list of xml
	 * resources in the spring context directory will be returned.
	 */
	protected String[] getApplicationContextLocations(Bundle bundle) {
		Dictionary manifestHeaders = bundle.getHeaders();
		String contextLocationsHeader = (String) manifestHeaders.get(CONTEXT_LOCATION_HEADER);
		if (contextLocationsHeader != null) {
			String[] locs = StringUtils.tokenizeToStringArray(contextLocationsHeader, CONTEXT_LOCATION_DELIMITERS);
			List ret = new ArrayList();
			for (int i = 0; i < locs.length; i++) {
				if (bundle.getEntry(locs[i]) != null) {
					ret.add(OsgiBundleResource.BUNDLE_URL_PREFIX + SPRING_CONTEXT_DIRECTORY + locs[i]);
				}
			}
			if (ret.isEmpty()) {
				return null;
			}
			else {
				return (String[]) ret.toArray();
			}
		}
		else {
			List resourceList = new ArrayList();
			Enumeration resources = bundle.findEntries(SPRING_CONTEXT_DIRECTORY, "*.xml", false);
			if (resources != null) {
				while (resources.hasMoreElements()) {
					URL resourceURL = (URL) resources.nextElement();
					resourceList.add(OsgiBundleResource.BUNDLE_URL_URL_PREFIX + resourceURL.toExternalForm());
				}
			}
			if (resourceList.isEmpty()) {
				return null;
			}
			else {
				String[] ret = new String[resourceList.size()];
				return (String[]) resourceList.toArray(ret);
				}
		}
	}

	private class UpdateThread extends Thread {
		private static final int SLEEP_TIME = 500;

		public void run() {
			while (true) {
				for (int i = 0; i < contexts.length; i++) {
					if (contexts[i].config.exists() && (contexts[i].config.lastModified() != contexts[i].lastModified)) {
						contexts[i].lastModified = contexts[i].config.lastModified();
						try {
							contexts[i].context.refresh();
						}
						catch (BeansException be) {
							log.error("Error updating configuration from spring.xml", be);
						}
					}
				}
				try {
					Thread.sleep(SLEEP_TIME);
				}
				catch (InterruptedException ignore) {
				}
			}
		}
	}
}
