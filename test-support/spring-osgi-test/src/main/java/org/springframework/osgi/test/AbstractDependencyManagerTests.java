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

import org.osgi.framework.Bundle;
import org.springframework.core.JdkVersion;
import org.springframework.core.io.Resource;
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

	// FIXME: externalize them
	protected String getSpringOSGiTestBundleUrl() {
		return "org.springframework.osgi,spring-osgi-test," + getSpringOsgiVersion();
	}

	protected String getSpringOSGiIoBundleUrl() {
		return "org.springframework.osgi,spring-osgi-io," + getSpringOsgiVersion();
	}

	protected String getSpringOSGiCoreBundleUrl() {
		return "org.springframework.osgi,spring-osgi-core," + getSpringOsgiVersion();
	}

	protected String getSpringOSGiExtenderBundleUrl() {
		return "org.springframework.osgi,spring-osgi-extender," + getSpringOsgiVersion();
	}

	protected String getSpringOSGiAnnotationBundleUrl() {
		return "org.springframework.osgi,spring-osgi-annotation," + getSpringOsgiVersion();
	}

	protected String getSpringCoreBundleUrl() {
		return "org.springframework,spring-core," + getSpringBundledVersion();
	}

	protected String getSpringBeansBundleUrl() {
		return "org.springframework,spring-beans," + getSpringBundledVersion();
	}

	protected String getJUnitLibUrl() {
		return "org.springframework.osgi,junit.osgi,3.8.1-SNAPSHOT";
	}

	/**
	 * Used only on JDK 1.4.
	 * 
	 * @return
	 */
	protected String getUtilConcurrentLibUrl() {
		return "org.springframework.osgi,backport-util-concurrent,3.0-SNAPSHOT";
	}

	protected String getSlf4jApi() {
		return "org.slf4j,slf4j-api," + SLF4J_VERSION;
	}

	protected String getJclOverSlf4jUrl() {
		return "org.slf4j,jcl104-over-slf4j," + SLF4J_VERSION;
	}

	protected String getSlf4jLog4jUrl() {
		return "org.slf4j,slf4j-log4j12," + SLF4J_VERSION;
	}

	protected String getLog4jLibUrl() {
		System.setProperty("log4j.ignoreTCL", "true");
		return "org.springframework.osgi,log4j.osgi,1.2.13-SNAPSHOT";
	}

	protected String getSpringMockUrl() {
		return "org.springframework,spring-test," + getSpringBundledVersion();
	}

	protected String getSpringContextUrl() {
		return "org.springframework,spring-context," + getSpringBundledVersion();
	}

	protected String getSpringAopUrl() {
		return "org.springframework,spring-aop," + getSpringBundledVersion();

	}

	protected String getSpringBeansUrl() {
		return "org.springframework,spring-beans," + getSpringBundledVersion();
	}

	protected String getAopAllianceUrl() {
		return "org.springframework.osgi,aopalliance.osgi,1.0-SNAPSHOT";
	}

	protected String getAsmLibrary() {
		return "org.springframework.osgi,asm.osgi,2.2.3-SNAPSHOT";
	}

	/**
	 * Mandator bundles (part of the test setup).
	 * 
	 * @return the array of mandatory bundle names
	 */
	protected String[] getMandatoryBundles() {

		String[] jars = new String[] { getSlf4jApi(), getJclOverSlf4jUrl(), getSlf4jLog4jUrl(), getLog4jLibUrl(),
				getJUnitLibUrl(), getSpringCoreBundleUrl(), getSpringBeansUrl(), getSpringContextUrl(),
				getSpringMockUrl(), getAopAllianceUrl(), getAsmLibrary(), getSpringAopUrl(),
				getSpringOSGiIoBundleUrl(), getSpringOSGiCoreBundleUrl(), getSpringBeansBundleUrl(),
				getSpringOSGiTestBundleUrl(), getSpringOSGiExtenderBundleUrl() };

		String[] bundles;

		if (JdkVersion.isAtLeastJava15()) {
			bundles = (String[]) ObjectUtils.addObjectToArray(jars, getSpringOSGiAnnotationBundleUrl());
		}
		else {
			bundles = (String[]) ObjectUtils.addObjectToArray(jars, getUtilConcurrentLibUrl());
		}

		return bundles;
	}

	public Bundle findBundleByLocation(String bundleLocation) {
		Bundle[] bundles = getBundleContext().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getLocation().equals(bundleLocation)) {
				return bundles[i];
			}
		}
		return null;
	}

	public Bundle findBundleBySymbolicName(String symbolicName) {
		Assert.hasText(symbolicName, "a not-null/not-empty symbolicName is required");
		Bundle[] bundles = getBundleContext().getBundles();
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

	/**
	 * Compatibility method - will be removed in the very near future.
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
