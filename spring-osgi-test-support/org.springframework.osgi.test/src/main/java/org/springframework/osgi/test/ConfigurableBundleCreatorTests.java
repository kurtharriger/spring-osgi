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
package org.springframework.osgi.test;

import java.io.InputStream;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.osgi.test.util.IOUtils;
import org.springframework.util.StringUtils;

/**
 * Subclass of OnTheFlyBundleCreatorTests which adds extra functionality for
 * configuring the jar creation. The created bundle (jar) can be configured by
 * indicating the locations for:
 * <ul>
 * <li>root folder - the starting point on which the resource patterns are
 * applied</li>
 * <li>inclusion patterns - comma separated strings which identify the
 * resources that should be included into the archive.</li>
 * <li>manifest - the location of the manifest used for testing.</li>
 * </ul>
 * <p/> These settings can be configured by:
 * <ul>
 * <li>using a properties file. By default the property name follows the
 * pattern "[testName]-bundle.properties", (i.e. /foo/bar/SomeTest will try to
 * load file /foo/bar/SomeTest-bundle.properties). If no properties file is
 * found, a set of defaults will be used.</li>
 * 
 * <li>overriding the default getXXX methods and providing an alternatve
 * implementation.</li>
 * </ul>
 * 
 * @author Costin Leau
 * 
 */
public abstract class ConfigurableBundleCreatorTests extends OnTheFlyBundleCreatorTests {

	protected static final String ROOT_DIR = "root.dir";
	protected static final String INCLUDE_PATTERNS = "include.patterns";
	protected static final String LIBS = "libs";
	protected static final String MANIFEST = "manifest";

	public static final Properties DEFAULT_SETTINGS = new Properties();

	static {
		DEFAULT_SETTINGS.setProperty(ROOT_DIR, "file:./target/test-classes");
		DEFAULT_SETTINGS.setProperty(INCLUDE_PATTERNS, "/**/*.class");
		DEFAULT_SETTINGS.setProperty(LIBS, "");
		DEFAULT_SETTINGS.setProperty(MANIFEST, "classpath:/org/springframework/osgi/test/MANIFEST.MF");
	}

	/**
	 * Settings for the jar creation.
	 */
	protected static Properties jarSettings;

	/**
	 * Resources' root path (the root path does not become part of the jar).
	 * 
	 * @return the root path
	 */
	protected String getRootPath() {
		return jarSettings.getProperty(ROOT_DIR);
	}

	/**
	 * Patterns for identifying the resources added to the jar. The patterns are
	 * added to the root path when performing the search.
	 * 
	 * @return the patterns
	 */
	protected String[] getBundleContentPattern() {
		return StringUtils.commaDelimitedListToStringArray(jarSettings.getProperty(INCLUDE_PATTERNS));
	}

	/**
	 * Return the location (in Spring resource style) of the manifest location
	 * to be used.
	 * 
	 * @return the manifest location
	 */
	protected String getManifestLocation() {
		return jarSettings.getProperty(MANIFEST);
	}

	/**
	 * Returns the settings location (by default, the test name; i.e.
	 * foo.bar.SomeTest will try to load foo/bar/SomeTest.properties).
	 * 
	 * @return
	 */
	protected String getSettingsLocation() {
		return getClass().getName().replace('.', '/') + "-bundle.properties";
	}

	/**
	 * Load the settings (if they are found). A non-null properties object will
	 * always be returned.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected Properties getSettings() throws Exception {
		Properties settings = new Properties(DEFAULT_SETTINGS);
		//settings.setProperty(ROOT_DIR, getRootPath());
		Resource resource = new ClassPathResource(getSettingsLocation());

		if (resource.exists()) {
			InputStream stream = resource.getInputStream();
			try {
				if (stream != null) {
					settings.load(stream);
					log.debug("loaded jar settings from " + getSettingsLocation());
				}
			}
			finally {
				IOUtils.closeStream(stream);
			}
		}
		else
			log.warn(getSettingsLocation() + " was not found; using defaults");

		return settings;
	}

	protected void postProcessBundleContext(BundleContext context) throws Exception {
		// hook in properties loading

		// reset the settings (useful when running multiple tests)
		jarSettings = null;
		// load settings
		jarSettings = getSettings();

		super.postProcessBundleContext(context);
	}

}
