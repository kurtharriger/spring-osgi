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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.osgi.OsgiException;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.web.deployer.ContextPathStrategy;
import org.springframework.osgi.web.deployer.WarDeployer;
import org.springframework.osgi.web.deployer.support.DefaultContextPathStrategy;
import org.springframework.osgi.web.deployer.tomcat.TomcatWarDeployer;
import org.springframework.osgi.web.extender.internal.scanner.DefaultWarScanner;
import org.springframework.osgi.web.extender.internal.scanner.WarScanner;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration class for the web extender. Takes care of locating the custom
 * configurations and merging the results with the defaults.
 * 
 * @author Costin Leau
 * 
 */
public class WarListenerConfiguration implements DisposableBean {

	/** logger */
	private static final Log log = LogFactory.getLog(WarListenerConfiguration.class);

	private static final String WAR_SCANNER_NAME = "warScanner";

	private static final String WAR_DEPLOYER_NAME = "warDeployer";

	private static final String CONTEXT_PATH_STRATEGY_NAME = "contextPathStrategy";

	private static final String PROPERTIES_NAME = "extenderProperties";

	private static final String UNDEPLOY_WARS_AT_SHUTDOWN = "undeploy.wars.at.shutdown";

	private static final String EXTENDER_CFG_LOCATION = "META-INF/spring/extender";

	/**
	 * old configuration location
	 * 
	 * @deprecated
	 */
	private static final String OLD_EXTENDER_CFG_LOCATION = "META-INF/spring";

	private static final String XML_PATTERN = "*.xml";

	private ConfigurableOsgiBundleApplicationContext extenderConfiguration;

	private WarScanner warScanner;

	private WarDeployer warDeployer;

	private ContextPathStrategy contextPathStrategy;

	private boolean undeployWarsAtShutdown;

	//
	// defaults
	//
	private static final boolean UNDEPLOY_WARS_AT_SHUTDOWN_DEFAULT = false;


	/**
	 * Constructs a new <code>WarListenerConfiguration</code> instance.
	 * Locates the extender configuration, creates an application context which
	 * will returned the extender items.
	 * 
	 * @param bundleContext extender OSGi bundle context
	 */
	public WarListenerConfiguration(BundleContext bundleContext) {
		Bundle bundle = bundleContext.getBundle();
		Properties properties = new Properties(createDefaultProperties());

		Enumeration enm = bundle.findEntries(EXTENDER_CFG_LOCATION, XML_PATTERN, false);

		Enumeration oldConfiguration = bundle.findEntries(OLD_EXTENDER_CFG_LOCATION, XML_PATTERN, false);
		
		if (enm == null && oldConfiguration == null) {
			log.info("No custom configuration detected; using defaults");

			warScanner = createDefaultWarScanner();
			warDeployer = createDefaultWarDeployer(bundleContext);
			contextPathStrategy = createDefaultContextPathStrategy();
		}
		else {
			String[] newConfigs = copyEnumerationToList(enm);
			String[] oldConfigs = copyEnumerationToList(oldConfiguration);

			if (!ObjectUtils.isEmpty(oldConfigs)) {
				log.warn("Extender configuration location [" + OLD_EXTENDER_CFG_LOCATION
						+ "] has been deprecated and will be removed after RC1; use [" + EXTENDER_CFG_LOCATION
						+ "] instead");
			}

			// merge old configs first so the new file can override bean definitions (if needed)
			String[] configs = StringUtils.mergeStringArrays(oldConfigs, newConfigs);

			
			log.info("Detected custom configurations " + ObjectUtils.nullSafeToString(configs));
			// create OSGi specific XML context
			ConfigurableOsgiBundleApplicationContext context = new OsgiBundleXmlApplicationContext(configs);
			context.setBundleContext(bundleContext);
			context.refresh();

			extenderConfiguration = context;

			warScanner = context.containsBean(WAR_SCANNER_NAME) ? (WarScanner) context.getBean(WAR_SCANNER_NAME,
				WarScanner.class) : createDefaultWarScanner();

			warDeployer = context.containsBean(WAR_DEPLOYER_NAME) ? (WarDeployer) context.getBean(WAR_DEPLOYER_NAME,
				WarDeployer.class) : createDefaultWarDeployer(bundleContext);

			contextPathStrategy = context.containsBean(CONTEXT_PATH_STRATEGY_NAME) ? (ContextPathStrategy) context.getBean(
				CONTEXT_PATH_STRATEGY_NAME, ContextPathStrategy.class)
					: createDefaultContextPathStrategy();

			// extender properties using the defaults as backup
			if (context.containsBean(PROPERTIES_NAME)) {
				Properties customProperties = (Properties) context.getBean(PROPERTIES_NAME, Properties.class);
				Enumeration propertyKey = customProperties.propertyNames();
				while (propertyKey.hasMoreElements()) {
					String property = (String) propertyKey.nextElement();
					properties.setProperty(property, customProperties.getProperty(property));
				}
			}

			undeployWarsAtShutdown = getUndeployWarsAtShutdown(properties);
		}
	}

	/**
	 * Copies the URLs returned by the given enumeration and returns them as an
	 * array of Strings for consumption by the application context.
	 * 
	 * @param enm
	 * @return
	 */
	private String[] copyEnumerationToList(Enumeration enm) {
		List urls = new ArrayList(4);
		while (enm.hasMoreElements()) {
			URL configURL = (URL) enm.nextElement();
			String configURLAsString = configURL.toExternalForm();
			try {
				urls.add(URLDecoder.decode(configURLAsString, "UTF8"));
			}
			catch (UnsupportedEncodingException uee) {
				log.warn("UTF8 encoding not supported, using the platform default");
				urls.add(URLDecoder.decode(configURLAsString));
			}
		}

		return (String[]) urls.toArray(new String[urls.size()]);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Cleanup the configuration items.
	 */
	public void destroy() {

		if (extenderConfiguration != null) {
			extenderConfiguration.close();
			extenderConfiguration = null;
		}
	}

	private Properties createDefaultProperties() {
		Properties properties = new Properties();
		properties.setProperty(UNDEPLOY_WARS_AT_SHUTDOWN, "" + UNDEPLOY_WARS_AT_SHUTDOWN_DEFAULT);
		return properties;
	}

	private WarScanner createDefaultWarScanner() {
		return new DefaultWarScanner();
	}

	private WarDeployer createDefaultWarDeployer(BundleContext bundleContext) {
		TomcatWarDeployer deployer = new TomcatWarDeployer();
		deployer.setBundleContext(bundleContext);
		try {
			deployer.afterPropertiesSet();
			return deployer;
		}
		catch (Exception ex) {
			throw new OsgiException("Cannot create Tomcat deployer", ex);
		}
	}

	private ContextPathStrategy createDefaultContextPathStrategy() {
		return new DefaultContextPathStrategy();
	}

	private boolean getUndeployWarsAtShutdown(Properties properties) {
		return Boolean.valueOf(properties.getProperty(UNDEPLOY_WARS_AT_SHUTDOWN)).booleanValue();
	}

	/**
	 * Returns the warScanner.
	 * 
	 * @return Returns the warScanner
	 */
	public WarScanner getWarScanner() {
		return warScanner;
	}

	/**
	 * Returns the warDeployer.
	 * 
	 * @return Returns the warDeployer
	 */
	public WarDeployer getWarDeployer() {
		return warDeployer;
	}

	/**
	 * Returns the contextPathStrategy.
	 * 
	 * @return Returns the contextPathStrategy
	 */
	public ContextPathStrategy getContextPathStrategy() {
		return contextPathStrategy;
	}

	/**
	 * Returns the undeployWarsAtShutdown.
	 * 
	 * @return Returns the undeployWarsAtShutdown
	 */
	public boolean shouldUndeployWarsAtShutdown() {
		return undeployWarsAtShutdown;
	}
}
