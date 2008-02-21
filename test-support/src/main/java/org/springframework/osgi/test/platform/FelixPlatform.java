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

package org.springframework.osgi.test.platform;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.main.Main;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.test.internal.util.IOUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Apache Felix (1.0.3+) OSGi platform.
 * 
 * @author Costin Leau
 * 
 */
public class FelixPlatform extends AbstractOsgiPlatform {

	private static final String BUNDLE_CONTEXT_METHOD = "getBundleContext";

	private static final String FELIX_PRIVATE_FIELD = "m_felix";

	private static final Log log = LogFactory.getLog(FelixPlatform.class);

	private static final String FELIX_LOG_LEVEL = "felix.log.level";

	private static final String FELIX_LOG_LEVEL_VALUE = "0";

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
		createStorageDir(props);
		// add logging
		props.put(FELIX_LOG_LEVEL, FELIX_LOG_LEVEL_VALUE);
		return props;
	}

	public BundleContext getBundleContext() {
		return context;
	}

	/**
	 * Configuration settings for the OSGi test run.
	 * 
	 * @return
	 */
	private void createStorageDir(Properties configProperties) {
		// create a temporary file if none is set
		if (felixStorageDir == null) {
			felixStorageDir = createTempDir("felix");
			felixStorageDir.deleteOnExit();

			if (log.isTraceEnabled())
				log.trace("Felix storage dir is " + felixStorageDir.getAbsolutePath());
		}

		configProperties.setProperty(FELIX_PROFILE_DIR_PROPERTY, this.felixStorageDir.getAbsolutePath());
	}

	public void start() throws Exception {
		// use Felix main and then read the felix instance

		// initialize properties and set them as system wide so Felix can pick them up
		System.getProperties().putAll(getPlatformProperties());

		Main.main(new String[0]);

		// read the Felix private field
		Field field = Main.class.getDeclaredField(FELIX_PRIVATE_FIELD);
		ReflectionUtils.makeAccessible(field);
		platform = (Felix) field.get(Main.class);

		// call getBundleContext
		final Method getContext = platform.getClass().getDeclaredMethod(BUNDLE_CONTEXT_METHOD, null);

		ReflectionUtils.makeAccessible(getContext);

		context = (BundleContext) getContext.invoke(platform, null);
	}

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
