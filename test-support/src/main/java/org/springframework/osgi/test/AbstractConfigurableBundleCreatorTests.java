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
import org.springframework.osgi.test.internal.util.IOUtils;
import org.springframework.osgi.test.internal.util.JarCreator;
import org.springframework.util.StringUtils;

/**
 * Subclass of AbstractOnTheFlyBundleCreatorTests which adds extra functionality
 * for configuring the jar creation. The created bundle (jar) can be configured
 * by indicating the locations for:
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
 * <li>overriding the default getXXX methods and providing an alternative
 * implementation.</li>
 * </ul>
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractConfigurableBundleCreatorTests extends AbstractOnTheFlyBundleCreatorTests {

	protected static final String ROOT_DIR = "root.dir";

	protected static final String INCLUDE_PATTERNS = "include.patterns";

	protected static final String LIBS = "libs";

	protected static final String MANIFEST = "manifest";

	protected static final Properties DEFAULT_SETTINGS = new Properties();

	static {
		DEFAULT_SETTINGS.setProperty(ROOT_DIR, "file:./target/test-classes");
		DEFAULT_SETTINGS.setProperty(INCLUDE_PATTERNS, JarCreator.EVERYTHING_PATTERN);
		DEFAULT_SETTINGS.setProperty(LIBS, "");
		DEFAULT_SETTINGS.setProperty(MANIFEST, "");
	}

	/**
	 * Settings for the jar creation. Static as it has to be cached between test
	 * runs.
	 */
	private static Properties jarSettings;


	/**
	 * Resources' root path (the root path does not become part of the jar).
	 * 
	 * @return the root path
	 */
	protected String getRootPath() {
		return jarSettings.getProperty(ROOT_DIR);
	}

	/**
	 * Ant-style patterns for identifying the resources added to the jar.The
	 * patterns are considered from the root path when performing the search.
	 * 
	 * <p/> By default, the content pattern is <code>*&#42;/*</code> which
	 * includes all sources from the root. One can configure the pattern to
	 * include specific files by using different patterns. For example, to
	 * include just the classes, XML and properties files one can use the
	 * following patterns:
	 * <ol>
	 * <li><code>*&#42;/*.class</code> for classes
	 * <li><code>*&#42;/*.xml</code> for XML files
	 * <li><code>*&#42;/*.properties</code> for properties files
	 * </ol>
	 * 
	 * @return array of Ant-style pattern
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
	 * foo.bar.SomeTest will try to load foo/bar/SomeTest-bundle.properties).
	 * 
	 * @return settings location for this test
	 */
	protected String getSettingsLocation() {
		return getClass().getName().replace('.', '/') + "-bundle.properties";
	}

	/**
	 * Returns the default settings used when creating the jar, in case the no
	 * customisations have been applied. Unless the base class is used as a
	 * testing framework, consider using a properties file for specifying
	 * specific properties for a test case.
	 * 
	 * @return default settings for creating the jar
	 * @see #getSettingsLocation()
	 */
	protected Properties getDefaultSettings() {
		return DEFAULT_SETTINGS;
	}

	/**
	 * Return the settings used for creating this jar. This method tries to
	 * locate and load the settings from the location indicated by
	 * {@link #getSettingsLocation()}. If no file is found, the default
	 * settings will be used. <p/> A non-null properties object will always be
	 * returned.
	 * 
	 * @return settings for creating the on the fly jar
	 * @throws Exception if loading the settings file fails
	 */
	protected Properties getSettings() throws Exception {
		Properties settings = new Properties(getDefaultSettings());
		// settings.setProperty(ROOT_DIR, getRootPath());
		Resource resource = new ClassPathResource(getSettingsLocation());

		if (resource.exists()) {
			InputStream stream = resource.getInputStream();
			try {
				if (stream != null) {
					settings.load(stream);
					logger.debug("loaded jar settings from " + getSettingsLocation());
				}
			}
			finally {
				IOUtils.closeStream(stream);
			}
		}
		else
			logger.info(getSettingsLocation() + " was not found; using defaults");

		return settings;
	}

	/**
	 * Load the jar settings, first from the disk falling back to the default
	 * settings, if none is found.
	 */
	protected void postProcessBundleContext(BundleContext context) throws Exception {
		// hook in properties loading

		// reset the settings (useful when running multiple tests)
		jarSettings = null;
		// load settings
		jarSettings = getSettings();
		// Somehow the JarCreator needs to get this
		jarCreator.setRootPath(getRootPath());

		super.postProcessBundleContext(context);
	}

}
