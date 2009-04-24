/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.io.internal.resolver;

import org.osgi.framework.Bundle;

/**
 * Importing bundle information.
 * 
 * @author Costin Leau
 * 
 */
public class ImportedBundle {

	private final Bundle importingBundle;

	private final String[] importedPackages;


	public ImportedBundle(Bundle importingBundle, String[] importedPackages) {
		super();
		this.importingBundle = importingBundle;
		this.importedPackages = importedPackages;
	}

	/**
	 * Returns the imported bundle.
	 * 
	 * @return importing bundle
	 */
	public Bundle getBundle() {
		return importingBundle;
	}

	/**
	 * 
	 * Returns an array of imported packages.
	 * 
	 * @return a non-null array of String representing the imported packages
	 */
	public String[] getImportedPackages() {
		return importedPackages;
	}

	public String toString() {
		return importingBundle.toString();
	}
}