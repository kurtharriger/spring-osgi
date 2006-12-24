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
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.MutablePropertyResolverImpl;
import org.apache.felix.main.Main;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Apache's Felix OSGi platform.
 * 
 * @author Costin Leau
 * 
 */
public class FelixPlatform implements OsgiPlatform {

	protected final Log log = LogFactory.getLog(getClass());

	private BundleContext context;
	private Felix platform;
	private File felixCacheDir;

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

		// specify a cache directory
		try {
			File tempFileName = File.createTempFile("org.springframework.osgi", "felix");
			tempFileName.delete(); // we want it to be a directory...
			this.felixCacheDir = new File(tempFileName.getAbsolutePath());
			this.felixCacheDir.mkdir();
			this.felixCacheDir.deleteOnExit();
			props.setProperty("felix.cache.dir", this.felixCacheDir.getAbsolutePath());
		}
		catch (IOException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Could not create temporary directory for Felix, using default", ex);
			}
		}
		
		// specify a cache profile
		props.setProperty("felix.cache.profile", "spring.osgi.junit.test");
		// embedded use
		props.setProperty("felix.embedded.execution", "true");

		// no auto-start
		props.setProperty("felix.auto.start.1", "");

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
	protected Properties getFelixConfiguration() throws Exception {
		// a naive solution since there can be multiple config.properties in the
		// classpath
		Resource resource = new ClassPathResource("config.properties");
		// used with Main
		System.getProperties().setProperty("felix.config.properties", resource.getURL().toExternalForm());

		// load config.properties (use Felix's Main for resolving placeholders)
		return Main.loadConfigProperties();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.platform.OsgiPlatform#start()
	 */
	public void start() throws Exception {

		Properties configProp = getFelixConfiguration();

		// add local settings
		configProp.putAll(getLocalConfiguration());

		// Main.main(new String[] {});
		// Field getFelix = Main.class.getDeclaredField("m_felix");
		// getFelix.setAccessible(true);
		// platform = (Felix) getFelix.get(null);

		platform = new Felix();
		platform.start(new MutablePropertyResolverImpl(configProp), null);

		Method getBundle = Felix.class.getDeclaredMethod("getBundle", new Class[] { long.class });
		getBundle.setAccessible(true);

		Bundle systemBundle = (Bundle) getBundle.invoke(platform, new Object[] { new Long(0) });
		// call getContext (part of BundleImpl class which has default
		// visibility)
		Method getContext = systemBundle.getClass().getSuperclass().getDeclaredMethod("getContext", null);
		getContext.setAccessible(true);
		context = (BundleContext) getContext.invoke(systemBundle, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.platform.OsgiPlatform#stop()
	 */
	public void stop() throws Exception {
		platform.shutdown();
	}
	
	public String toString() {
		return "Felix OSGi Platform";
	}

}
