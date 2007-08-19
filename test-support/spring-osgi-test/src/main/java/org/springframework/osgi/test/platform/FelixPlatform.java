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
 */
package org.springframework.osgi.test.platform;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.main.Main;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.test.util.IOUtils;
import org.springframework.util.ClassUtils;

/**
 * Apache's Felix OSGi platform.
 * 
 * @author Costin Leau
 * 
 */
public class FelixPlatform extends AbstractOsgiPlatform {

	private static final Log log = LogFactory.getLog(FelixPlatform.class);

	private static final String FELIX_CONF_FILE = "felix.config.properties";

	private static final String FELIX_CONFIG_PROPERTY = "felix.config.properties";

	private static final String FELIX_PROFILE_DIR_PROPERTY = "felix.cache.profiledir";

	private BundleContext context;

	private Felix platform;

	private File felixStorageDir;

	public FelixPlatform() {
		toString = "Felix OSGi Platform";
	}

	protected Properties getPlatformProperties() {
		// load Felix configuration
		Properties props = new Properties();
		props.putAll(getFelixConfiguration());
		props.putAll(getLocalConfiguration());
		return props;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.platform.OsgiPlatform#getBundleContext()
	 */
	public BundleContext getBundleContext() {
		return context;
	}

	/**
	 * Configuration settings for the OSGi test run.
	 * 
	 * @return
	 */
	protected Properties getLocalConfiguration() {
		Properties props = new Properties();

		felixStorageDir = createTempDir("felix");
		props.setProperty(FELIX_PROFILE_DIR_PROPERTY, this.felixStorageDir.getAbsolutePath());
		if (log.isTraceEnabled())
			log.trace("felix storage dir is " + felixStorageDir.getAbsolutePath());

		return props;
	}

	/**
	 * Load Felix config.properties.
	 * 
	 * <strong>Note</strong> the current implementation uses Felix's Main class
	 * to resolve placeholders as opposed to loading the properties manually
	 * (through JDK's Properties class or Spring's PropertiesFactoryBean).
	 * 
	 * @return
	 */
	protected Properties getFelixConfiguration() {
		String location = "/".concat(ClassUtils.classPackageAsResourcePath(getClass())).concat("/").concat(
			FELIX_CONF_FILE);
		URL url = getClass().getResource(location);
		if (url == null)
			throw new RuntimeException("cannot find felix configuration properties file:" + location);

		// used with Main
		System.getProperties().setProperty(FELIX_CONFIG_PROPERTY, url.toExternalForm());

		// load config.properties (use Felix's Main for resolving placeholders)
		return Main.loadConfigProperties();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.platform.OsgiPlatform#start()
	 */
	public void start() throws Exception {

		platform = new Felix(getConfigurationProperties(), null);
		platform.start();

		Bundle systemBundle = platform;

		// call getBundleContext
		Method getContext = systemBundle.getClass().getDeclaredMethod("getBundleContext", null);
		getContext.setAccessible(true);
		context = (BundleContext) getContext.invoke(systemBundle, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.platform.OsgiPlatform#stop()
	 */
	public void stop() throws Exception {
		try {
			platform.stop();
		}
		finally {
			// remove cache folder
			IOUtils.delete(felixStorageDir);
		}
	}

}
