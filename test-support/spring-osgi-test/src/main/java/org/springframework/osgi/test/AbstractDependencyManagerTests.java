/*
 * Copyright 2002-2007 the original author or authors.
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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.JdkVersion;
import org.springframework.core.io.Resource;
import org.springframework.osgi.test.util.PropertiesUtil;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Dependency manager layer - uses iternally an {@link ArtifactLocator} to
 * retrieve the required dependencies for the running test.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractDependencyManagerTests extends AbstractSynchronizedOsgiTests {

	private static final String MANDATORY_FILE_CONF = "/org/springframework/osgi/test/boot-bundles.properties";

	private static final String IGNORE = "ignore";

	/**
	 * Artifact locator (by default the Local Maven repo).
	 */
	private ArtifactLocator locator = new LocalFileSystemMavenRepository();

	public AbstractDependencyManagerTests() {
		super();
	}

	public AbstractDependencyManagerTests(String name) {
		super(name);
	}

	private static final String SPRING_OSGI_VERSION = "1.0-m3-SNAPSHOT";

	private static final String SPRING_BUNDLED_VERSION = "2.1-m4";

	private static final String SLF4J_VERSION = "1.4.3";

	/**
	 * Return the Spring/OSGi version used by the core bundles.
	 * @return
	 */
	protected String getSpringOsgiVersion() {
		return SPRING_OSGI_VERSION;
	}

	/**
	 * Return the Spring osgified version used by the test core bundles.
	 * 
	 * @return
	 */
	protected String getSpringBundledVersion() {
		return SPRING_BUNDLED_VERSION;
	}

	protected String getMandatoryBundlesConfigurationFile() {
		return MANDATORY_FILE_CONF;
	}

	protected String[] getMandatoryBundles() {

		// load properties file
		Properties props = PropertiesUtil.loadAndExpand(getClass().getResourceAsStream(
			getMandatoryBundlesConfigurationFile()));

		if (props == null)
			throw new IllegalArgumentException("cannot load default configuration from "
					+ getMandatoryBundlesConfigurationFile());

		boolean trace = logger.isTraceEnabled();

		if (trace)
			logger.trace("loaded properties " + props);

		Properties excluded = PropertiesUtil.filterKeysStartingWith(props, IGNORE);

		if (trace) {
			logger.trace("excluded ignored properties " + excluded);
		}

		// filter based on JDK codes
		int jdkVersion = JdkVersion.getMajorJavaVersion();

		int filteredVersion = JdkVersion.JAVA_14;
		do {
			String excludedValue = "-" + filteredVersion;
			// filter based on detected JDK
			excluded = PropertiesUtil.filterValuesStartingWith(props, excludedValue);
			if (trace)
				logger.trace("JDK " + filteredVersion + " excluded bundles " + excluded);
			filteredVersion++;

		} while (filteredVersion <= jdkVersion);

		String[] bundles = (String[]) props.keySet().toArray(new String[props.size()]);
		if (logger.isDebugEnabled())
			logger.debug("loaded bundles " + ObjectUtils.nullSafeToString(bundles));

		return bundles;
	}

	// Set log4j property to avoid TCCL problems during startup
	protected void preProcessBundleContext(BundleContext platformBundleContext) throws Exception {
		System.setProperty("log4j.ignoreTCL", "true");
		super.preProcessBundleContext(platformBundleContext);
	}

	// protected Bundle findBundleByLocation(String bundleLocation) {
	// Bundle[] bundles = bundleContext.getBundles();
	// for (int i = 0; i < bundles.length; i++) {
	// if (bundles[i].getLocation().equals(bundleLocation)) {
	// return bundles[i];
	// }
	// }
	// return null;
	// }

	protected Bundle findBundleBySymbolicName(String symbolicName) {
		Assert.hasText(symbolicName, "a not-null/not-empty symbolicName isrequired");
		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (symbolicName.equals(bundles[i].getSymbolicName())) {
				return bundles[i];
			}
		}
		return null;
	}

	/**
	 * Concrete implementation for locate Bundle. The given bundleId should be
	 * in CSV format, specifying the artifact group, id, version and optionally
	 * the type.
	 * 
	 */
	protected Resource locateBundle(String bundleId) {
		Assert.hasText(bundleId, "bundleId should not be empty");

		// parse the String
		String[] artifactId = StringUtils.commaDelimitedListToStringArray(bundleId);

		Assert.isTrue(artifactId.length >= 3, "the CSV string " + bundleId + " contains too few values");
		// TODO: add a smarter mechanism which can handle 1 or 2 values CSVs

		for (int i = 0; i < artifactId.length; i++) {
			artifactId[i] = StringUtils.trimWhitespace(artifactId[i]);
		}

		return (artifactId.length == 3 ? locator.locateArtifact(artifactId[0], artifactId[1], artifactId[2])
				: locator.locateArtifact(artifactId[0], artifactId[1], artifactId[2], artifactId[3]));
	}

	/**
	 * @param locator The locator to set.
	 */
	public void injectLocator(ArtifactLocator locator) {
		this.locator = locator;
	}

	/**
	 * @return Returns the locator.
	 */
	public ArtifactLocator getLocator() {
		return locator;
	}

	//
	// FIXME: remove these methods after M3
	//

	/**
	 * Compatibility method - will be removed in the very near future.
	 * @deprecated this method will be removed after M3; use
	 * {@link #getLocator() and ArtifactLocator#locateArtifact(String, String, String) method instead or
	 * simply #locateBundle(String)
	 * 
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @return
	 */
	protected String localMavenArtifact(String groupId, String artifactId, String version) {
		return groupId + "," + artifactId + "," + version;
	}

	/**
	 * Compatibility method - will be removed in the very near future.
	 * 
	 * @deprecated this method will be removed after M3; use
	 * {@link #getLocator() and ArtifactLocator#locateArtifact(String, String, String) method instead or
	 * simply #locateBundle(String)
	 * 
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param type
	 * @return
	 */
	protected String localMavenArtifact(String groupId, String artifactId, String version, String type) {
		return groupId + "," + artifactId + "," + version + "," + type;
	}
}
