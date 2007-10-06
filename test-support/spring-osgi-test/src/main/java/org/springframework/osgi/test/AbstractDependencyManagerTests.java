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

import org.osgi.framework.BundleContext;
import org.springframework.core.JdkVersion;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.osgi.internal.test.provisioning.LocalFileSystemMavenRepository;
import org.springframework.osgi.internal.test.util.PropertiesUtil;
import org.springframework.osgi.test.provisioning.ArtifactLocator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Dependency manager layer - uses internally an {@link ArtifactLocator} to
 * retrieve the required dependencies for the running test.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractDependencyManagerTests extends AbstractSynchronizedOsgiTests {

	private static final String TEST_FRRAMEWORK_BUNDLES_CONF_FILE = "/org/springframework/osgi/internal/test/boot-bundles.properties";

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

	private static final String SPRING_OSGI_VERSION = "1.0-m3";

	private static final String SPRING_BUNDLED_VERSION = "2.1-m4";

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

	/**
	 * Bundles that have to be installed as part of the test setup. This method
	 * provides an alternative to {@link #getTestBundles()} as it allows
	 * subclasses to specify just the bundle name w/o worrying about locating
	 * the artifact (which is resolved through the {@link ArtifactLocator}).
	 * 
	 * <p/> A bundle name can have any value and depends on the format expected
	 * by the {@link ArtifactLocator} implementation. By default, a CSV format
	 * is expected.
	 * 
	 * <p/> This method allows a declarative approach in declaring bundles as
	 * opposed to {@link #getTestBundles()} which provides a programmatic one.
	 * 
	 * @see #locateBundle(String)
	 * @return an array of bundle identificators
	 */
	protected String[] getTestBundlesNames() {
		return getBundles();
	}

	/**
	 * Declarative method indicating the bundles required by the test framework,
	 * by their names rather then as {@link Resource}s.
	 * 
	 * <p/> This implementation reads a predefined properties file to determine
	 * the bundles needed.
	 * 
	 * <p/> This method allows a declarative approach in declaring bundles as
	 * opposed to {@link #getTestBundles()} which provides a programmatic one.
	 * 
	 * @see #getTestingFrameworkBundlesConfiguration()
	 * @see #locateBundle(String)
	 * @return an array of bundle identificators
	 */
	protected String[] getTestFrameworkBundlesNames() {
		// load properties file
		Properties props = PropertiesUtil.loadAndExpand(getTestingFrameworkBundlesConfiguration());

		if (props == null)
			throw new IllegalArgumentException("cannot load default configuration from "
					+ getTestingFrameworkBundlesConfiguration());

		boolean trace = logger.isTraceEnabled();

		if (trace)
			logger.trace("loaded properties " + props);

		Properties excluded = PropertiesUtil.filterKeysStartingWith(props, IGNORE);

		if (trace) {
			logger.trace("excluded ignored properties " + excluded);
		}

		// filter bundles which are Tiger/JDK 1.5 specific
		String sign = null;
		if (JdkVersion.isAtLeastJava15()) {
			sign = "-15";
		}
		else {
			sign = "+15";
		}

		excluded = PropertiesUtil.filterValuesStartingWith(props, sign);
		if (trace)
			logger.trace("JDK " + JdkVersion.getJavaVersion() + " excluded bundles " + excluded);

		String[] bundles = (String[]) props.keySet().toArray(new String[props.size()]);
		if (logger.isDebugEnabled())
			logger.debug("loaded bundles " + ObjectUtils.nullSafeToString(bundles));

		return bundles;
	}

	/**
	 * Return the location of the test framework bundles configuration.
	 * 
	 * @return location of the test framework bundles configuration
	 */
	protected Resource getTestingFrameworkBundlesConfiguration() {
		return new InputStreamResource(getClass().getResourceAsStream(TEST_FRRAMEWORK_BUNDLES_CONF_FILE));
	}

	/**
	 * Default implementation that uses the {@link ArtifactLocator} to resolve
	 * the bundles specified in {@link #getTestBundlesNames()}.
	 * 
	 * Subclasses that override this method should decide whether they want to
	 * support {@link #getTestBundlesNames()} or not.
	 * 
	 * @see org.springframework.osgi.test.AbstractOsgiTests#getTestBundles()
	 */
	protected Resource[] getTestBundles() {
		return locateBundles(getTestBundlesNames());
	}

	/**
	 * Default implementation that uses {@link #getTestFrameworkBundlesNames()}
	 * to discover the bundles part of the testing framework.
	 * 
	 * @see org.springframework.osgi.test.AbstractOsgiTests#getTestFrameworkBundles()
	 */
	protected Resource[] getTestFrameworkBundles() {
		return locateBundles(getMandatoryBundles());
	}

	/**
	 * Utility method that loads the bundles given as strings. Will delegate to
	 * {@link #locateBundle(String)}.
	 * 
	 * @param bundles
	 * @return
	 */
	private Resource[] locateBundles(String[] bundles) {
		if (bundles == null)
			bundles = new String[0];
		
		Resource[] res = new Resource[bundles.length];
		for (int i = 0; i < bundles.length; i++) {
			res[i] = locateBundle(bundles[i]);
		}
		return res;
	}

	// Set log4j property to avoid TCCL problems during startup
	protected void preProcessBundleContext(BundleContext platformBundleContext) throws Exception {
		System.setProperty("log4j.ignoreTCL", "true");
		super.preProcessBundleContext(platformBundleContext);
	}

	/**
	 * Locate (through the {@link ArtifactLocator}) an OSGi bundle given as a
	 * String.
	 * 
	 * The default implementation expects the argument to be in Comma Separated
	 * Values (CSV) format which indicates an artifact group, id, version and
	 * optionally the type.
	 * 
	 * @param bundleId the bundle identificator in CSV format
	 * @return a resource pointing to the artifact location
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

	/**
	 * @deprecated use {@link #getTestFrameworkBundlesNames()} instead.
	 */
	protected String[] getMandatoryBundles() {
		return getTestFrameworkBundlesNames();
	}

	/**
	 * @deprecated no replacement provided for it
	 * @see org.springframework.osgi.test.AbstractOsgiTests#getBundleLocations()
	 */
	protected String[] getBundleLocations() {
		return super.getBundleLocations();
	}

	/**
	 * @deprecated use {@link #getTestBundlesNames()} instead.
	 * @see org.springframework.osgi.test.AbstractOsgiTests#getBundles()
	 */
	protected String[] getBundles() {
		return new String[0];
	}

}
