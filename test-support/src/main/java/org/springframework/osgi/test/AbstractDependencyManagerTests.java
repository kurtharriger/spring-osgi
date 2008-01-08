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
import org.springframework.osgi.test.internal.util.PropertiesUtil;
import org.springframework.osgi.test.provisioning.ArtifactLocator;
import org.springframework.osgi.test.provisioning.internal.LocalFileSystemMavenRepository;
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

	private static final String TEST_FRRAMEWORK_BUNDLES_CONF_FILE = "/org/springframework/osgi/test/internal/boot-bundles.properties";

	private static final String IGNORE = "ignore";

	/**
	 * Artifact locator (by default the Local Maven repository).
	 */
	private ArtifactLocator locator = new LocalFileSystemMavenRepository();


	public AbstractDependencyManagerTests() {
		super();
	}

	public AbstractDependencyManagerTests(String name) {
		super(name);
	}


	private static final String SPRING_OSGI_VERSION_PROP_KEY = "ignore.spring.osgi.version";

	private static final String SPRING_VERSION_PROP_KEY = "ignore.spring.version";

	/** uninitialised - read from the properties file */
	private String springOsgiVersion = null;

	/** uninitialised - read from the properties file */
	private String springBundledVersion = null;


	/**
	 * Return the Spring-DM version used by the core bundles.
	 * 
	 * @return
	 */
	protected String getSpringDMVersion() {
		if (springOsgiVersion == null) {
			springOsgiVersion = System.getProperty(SPRING_OSGI_VERSION_PROP_KEY);
		}

		return springOsgiVersion;
	}

	/**
	 * Return the Spring osgified version used by the test core bundles.
	 * 
	 * @return
	 */
	protected String getSpringVersion() {
		if (springBundledVersion == null) {
			springBundledVersion = System.getProperty(SPRING_VERSION_PROP_KEY);
		}
		return springBundledVersion;
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
	 * @return an array of bundle identifiers
	 */
	protected String[] getTestBundlesNames() {
		return new String[0];
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
	 * @return an array of bundle identifiers
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

		// pass properties to test instance running inside OSGi space
		System.getProperties().put(SPRING_OSGI_VERSION_PROP_KEY, props.get(SPRING_OSGI_VERSION_PROP_KEY));
		System.getProperties().put(SPRING_VERSION_PROP_KEY, props.get(SPRING_VERSION_PROP_KEY));

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
		return locateBundles(getTestFrameworkBundlesNames());
	}

	/**
	 * Utility method that loads the bundles given as strings. Will delegate to
	 * {@link #locateBundle(String)}.
	 * 
	 * @param bundles
	 * @return
	 */
	protected Resource[] locateBundles(String[] bundles) {
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
	 * @param bundleId the bundle identifier in CSV format
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
	 * Returns the ArtifactLocator used by this test suite.
	 * Subclasses should override this method if the default
	 * locator (searching the local Maven2 repository) is not
	 * enough.
	 * 
	 * @return Returns the locator.
	 */
	protected ArtifactLocator getLocator() {
		return locator;
	}
}
