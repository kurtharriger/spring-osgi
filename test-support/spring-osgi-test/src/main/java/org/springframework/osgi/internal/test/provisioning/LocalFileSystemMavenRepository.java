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
package org.springframework.osgi.internal.test.provisioning;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.osgi.test.provisioning.ArtifactLocator;

/**
 * Locator for artifacts found in the local maven repository. Does <strong>not</strong>
 * use Maven libraries, it rather uses the maven patterns and conventions to
 * identify the artifacts.
 * 
 * @author Hal Hidlebrand
 * @author Costin Leau
 * 
 */
public class LocalFileSystemMavenRepository implements ArtifactLocator {

	private static final Log log = LogFactory.getLog(LocalFileSystemMavenRepository.class);

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
	public Resource locateArtifact(String groupId, String artifactId, String version) {
		return locateArtifact(groupId, artifactId, version, DEFAULT_ARTIFACT_TYPE);
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
	 * @return
	 */
	public Resource locateArtifact(String groupId, String artifactId, String version, String type) {
		try {
			return localMavenBuildArtifact(artifactId, version, type);
		}
		catch (IllegalStateException illStateEx) {
			if (log.isDebugEnabled())
				log.debug("local maven build artifact detection failed, falling back to local maven bundle");
			return localMavenBundle(groupId, artifactId, version, type);
		}
	}

	/**
	 * Return the resource of the indicated bundle in the local Maven repository
	 * 
	 * @param groupId - the groupId of the organization supplying the bundle
	 * @param artifact - the artifact id of the bundle
	 * @param version - the version of the bundle
	 * @return
	 */
	protected Resource localMavenBundle(String groupId, String artifact, String version, String type) {
		String defaultHome = new File(new File(System.getProperty("user.home")), ".m2/repository").getAbsolutePath();
		File repositoryHome = new File(System.getProperty("localRepository", defaultHome));

		StringBuffer location = new StringBuffer(groupId.replace('.', '/'));
		location.append('/');
		location.append(artifact);
		location.append('/');
		location.append(version);
		location.append('/');
		location.append(artifact);
		location.append('-');
		location.append(version);
		location.append(".");
		location.append(type);

		return new FileSystemResource(new File(repositoryHome, location.toString()));
	}

	/**
	 * Find a local maven artifact in the current build tree. This searches for
	 * resources produced by the package phase of a maven build.
	 * 
	 * @param artifactId
	 * @param version
	 * @param type
	 * @return
	 */
	protected Resource localMavenBuildArtifact(String artifactId, String version, String type) {
		try {
			File found = new MavenPackagedArtifactFinder(artifactId, version, type).findPackagedArtifact(new File("."));
			Resource res = new FileSystemResource(found);
			if (log.isDebugEnabled()) {
				log.debug("found local maven artifact:" + res.getDescription() + " for " + artifactId + "|" + version);
			}
			return res;
		}
		catch (IOException ioEx) {
			throw (RuntimeException) new IllegalStateException("Artifact " + artifactId + "-" + version + "." + type
					+ " could not be found").initCause(ioEx);
		}
	}

}
