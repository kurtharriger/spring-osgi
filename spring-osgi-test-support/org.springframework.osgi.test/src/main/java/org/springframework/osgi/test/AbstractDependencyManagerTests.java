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

import java.io.File;
import java.io.IOException;

import org.osgi.framework.Bundle;

/**
 * @author Costin Leau
 * 
 */
public abstract class AbstractDependencyManagerTests extends AbstractSynchronizedOsgiTests {

	public AbstractDependencyManagerTests() {
		super();
	}

	public AbstractDependencyManagerTests(String name) {
		super(name);
	}

	// FIXME: externalize them
	protected String getSpringOSGiTestBundleUrl() {
		return localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test", "1.0-SNAPSHOT");
	}

	protected String getSpringOSGiIoBundleUrl() {
		return localMavenArtifact("org.springframework.osgi", "spring-osgi-io", "1.0-SNAPSHOT");
	}

	protected String getSpringCoreBundleUrl() {
		return localMavenArtifact("org.springframework.osgi", "spring-core", "2.1-SNAPSHOT");
	}

	protected String getJUnitLibUrl() {
		return localMavenArtifact("org.springframework.osgi", "junit.osgi", "3.8.1-SNAPSHOT");
	}

	protected String getUtilConcurrentLibUrl() {
		return localMavenArtifact("org.springframework.osgi", "backport-util-concurrent", "3.0-SNAPSHOT");
	}

	protected String getSlf4jApiUrl() {
		return localMavenArtifact("org.slf4j", "slf4j-api", "1.3.0");
	}

	protected String getJclOverSlf4jUrl() {
		return localMavenArtifact("org.slf4j", "jcl104-over-slf4j", "1.3.0");
	}

	protected String getSlf4jLog4jUrl() {
		return localMavenArtifact("org.slf4j", "slf4j-log4j12", "1.3.0");
	}

	protected String getLog4jLibUrl() {
		System.setProperty("log4j.ignoreTCL", "true");
		return localMavenArtifact("org.springframework.osgi", "log4j.osgi", "1.2.13-SNAPSHOT");
	}

	protected String getSpringMockUrl() {
		return localMavenArtifact("org.springframework.osgi", "spring-mock", "2.1-SNAPSHOT");
	}

	protected String getSpringContextUrl() {
		return localMavenArtifact("org.springframework.osgi", "spring-context", "2.1-SNAPSHOT");
	}

	protected String getSpringBeansUrl() {
		return localMavenArtifact("org.springframework.osgi", "spring-beans", "2.1-SNAPSHOT");
	}

	protected String getAopAllianceUrl() {
		return localMavenArtifact("org.springframework.osgi", "aopalliance.osgi", "1.0-SNAPSHOT");
	}

	/**
	 * Find a local maven artifact. First tries to find the resource as a
	 * packaged artifact produced by a local maven build, and if that fails will
	 * search the local maven repository.
	 * 
	 * @param groupId - the groupId of the organization supplying the bundle
	 * @param artifactId - the artifact id of the bundle
	 * @param version - the version of the bundle
	 * @return the String representing the URL location of this bundle
	 */
	protected String localMavenArtifact(String groupId, String artifactId, String version) {
		return localMavenArtifact(groupId, artifactId, version, "jar");
	}

	/**
	 * Find a local maven artifact. First tries to find the resource as a
	 * packaged artifact produced by a local maven build, and if that fails will
	 * search the local maven repository.
	 * 
	 * @param groupId - the groupId of the organization supplying the bundle
	 * @param artifactId - the artifact id of the bundle
	 * @param version - the version of the bundle
	 * @param type - the extension type of the artifact
	 * @return the String representing the URL location of this bundle
	 */
	protected String localMavenArtifact(String groupId, String artifactId, String version, String type) {
		try {
			return localMavenBuildArtifact(artifactId, version, type);
		}
		catch (IllegalStateException illStateEx) {
			return localMavenBundle(groupId, artifactId, version, type);
		}
	}

	/**
	 * Answer the url string of the indicated bundle in the local Maven
	 * repository
	 * 
	 * @param groupId - the groupId of the organization supplying the bundle
	 * @param artifact - the artifact id of the bundle
	 * @param version - the version of the bundle
	 * @return the String representing the URL location of this bundle
	 */
	protected String localMavenBundle(String groupId, String artifact, String version, String type) {
		String defaultHome = new File(new File(System.getProperty("user.home")), ".m2/repository").getAbsolutePath();
		File repositoryHome = new File(System.getProperty("localRepository", defaultHome));

		String location = groupId.replace('.', '/');
		location += '/';
		location += artifact;
		location += '/';
		location += version;
		location += '/';
		location += artifact;
		location += '-';
		location += version;
		location += ".";
		location += type;
		return "file:" + new File(repositoryHome, location).getAbsolutePath();
	}

	/**
	 * Find a local maven artifact in the current build tree. This searches for
	 * resources produced by the package phase of a maven build.
	 * 
	 * @param artifactId
	 * @param version
	 * @param type
	 * @return a String representing the URL location of this bundle
	 */
	protected String localMavenBuildArtifact(String artifactId, String version, String type) {
		try {
			File found = new MavenPackagedArtifactFinder(artifactId, version, type).findPackagedArtifact(new File("."));
			String path = found.toURL().toExternalForm();
			if (log.isDebugEnabled()) {
				log.debug("found local maven artifact " + path + " for " + artifactId + "|" + version);
			}
			return path;
		}
		catch (IOException ioEx) {
			throw (RuntimeException) new IllegalStateException("Artifact " + artifactId + "-" + version + "." + type
					+ " could not be found").initCause(ioEx);
		}
	}

	/**
	 * Mandator bundles (part of the test setup).
	 * 
	 * @return the array of mandatory bundle names (sans Log4J, which gets
	 * special handling
	 */
	protected String[] getMandatoryBundles() {
		return new String[] { getJUnitLibUrl(), getSlf4jApiUrl(), getJclOverSlf4jUrl(), getSlf4jLog4jUrl(), getLog4jLibUrl(), 
				getSpringCoreBundleUrl(), getSpringBeansUrl(), getSpringContextUrl(),
				getSpringMockUrl(), getUtilConcurrentLibUrl(), getSpringOSGiIoBundleUrl(), getSpringOSGiTestBundleUrl() };
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

	public Bundle findBundleBySymbolicName(String sybmolicName) {
		Bundle[] bundles = getBundleContext().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getSymbolicName().equals(sybmolicName)) {
				return bundles[i];
			}
		}
		return null;
	}

}
