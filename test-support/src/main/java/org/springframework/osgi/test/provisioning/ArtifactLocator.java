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
package org.springframework.osgi.test.provisioning;

import org.springframework.core.io.Resource;

/**
 * Interface describing the contract for finding dependencies artifacts.
 * Implementations can rely on various lookup strategies for finding the actual
 * artifacts (i.e. Maven, Ant, Ivy, etc...)
 * 
 * @author Costin Leau
 * 
 */
public interface ArtifactLocator {

	String DEFAULT_ARTIFACT_TYPE = "jar";
	
	/**
	 * Locate the artifact under the given group, with the given id, version and
	 * type. Implementations are free to provide defaults, in case null values
	 * are passed in. The only required field is #id.
	 * 
	 * @param group artifact group
	 * @param id artifact id or name
	 * @param version artifact version
	 * @param type artifact type
	 * 
	 * @return
	 */
	Resource locateArtifact(String group, String id, String version, String type);

	/**
	 * Shortcut version which uses the implementation default artifact type
	 * {@link #DEFAULT_ARTIFACT_TYPE}.
	 * 
	 * @param group
	 * @param id
	 * @param version
	 * @return
	 */
	Resource locateArtifact(String group, String id, String version);
}
