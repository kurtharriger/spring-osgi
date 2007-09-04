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
package org.springframework.osgi.internal.service;

/**
 * Interface describing the contract of OSGi service importers. Used for
 * decoupling other packages from using the OSGi service exporters directly.
 * 
 * @author Costin Leau
 * 
 */
public interface ServiceImporter {

	/**
	 * Does this importer have mandatory dependencies?
	 * 
	 * @return true if dependencies are mandatory, false otherwise.
	 */
	boolean isMandatory();

	/**
	 * Register a {@link MandatoryDependencyListener} on this importer.
	 * 
	 * @param listener
	 */
	void registerListener(MandatoryDependencyListener listener);
}
