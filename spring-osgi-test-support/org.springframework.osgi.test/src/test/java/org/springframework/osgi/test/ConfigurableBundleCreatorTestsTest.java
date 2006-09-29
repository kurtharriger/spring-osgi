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

import java.util.Properties;

import org.springframework.core.io.ClassPathResource;

import junit.framework.TestCase;

/**
 * Test Case for ConfigurableBundleCreatorTests.
 * 
 * @author Costin Leau
 * 
 */
public class ConfigurableBundleCreatorTestsTest extends TestCase {

	private ConfigurableBundleCreatorTests bundleCreator;

	protected void setUp() throws Exception {
		bundleCreator = new ConfigurableBundleCreatorTests() {
		};
	}

	protected void tearDown() throws Exception {
		bundleCreator = null;
	}

	public void testGetSettingsLocation() throws Exception {

		assertEquals(bundleCreator.getClass().getPackage().getName().replace('.', '/')
				+ "/ConfigurableBundleCreatorTestsTest$1-bundle.properties", bundleCreator.getSettingsLocation());
	}

	public void testDefaultJarSettings() throws Exception {

		Properties defaultSettings = bundleCreator.getSettings();
		ConfigurableBundleCreatorTests.jarSettings = defaultSettings;
		assertNotNull(defaultSettings);
		assertNotNull(bundleCreator.getRootPath());
		assertNotNull(bundleCreator.getBundleContentPattern());
		assertNotNull(bundleCreator.getManifestLocation());
	}

	public void testPropertiesLoading() throws Exception {
		Properties testSettings = bundleCreator.getSettings();

		Properties props = new Properties();
		props.load(new ClassPathResource("org/springframework/osgi/test/ConfigurableBundleCreatorTestsTest$1-bundle.properties").getInputStream());

		assertEquals(props.getProperty(ConfigurableBundleCreatorTests.INCLUDE_PATTERNS),
				testSettings.getProperty(ConfigurableBundleCreatorTests.INCLUDE_PATTERNS));
		assertEquals(props.getProperty(ConfigurableBundleCreatorTests.MANIFEST),
				testSettings.getProperty(ConfigurableBundleCreatorTests.MANIFEST));
	}

	public void testJarCreation() throws Exception {

	}
}
