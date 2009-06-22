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
package org.springframework.osgi.extender.internal.blueprint.activator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.extender.internal.activator.TypeCompatibilityChecker;

/**
 * Basic type compatibility checker
 * @author Costin Leau
 */
class BlueprintTypeCompatibilityChecker implements TypeCompatibilityChecker {

	// container package check
	private static final String CONTAINER_PKG_CLASS = "org.osgi.service.blueprint.container.BlueprintContainer";
	// reflect package check
	private static final String REFLECT_PKG_CLASS = "org.osgi.service.blueprint.reflect.ComponentMetadata";

	private final Class<?> containerPkgClass;
	private final Class<?> reflectPkgClass;

	public BlueprintTypeCompatibilityChecker(Bundle extenderBundle) {
		try {
			containerPkgClass = extenderBundle.loadClass(CONTAINER_PKG_CLASS);
			reflectPkgClass = extenderBundle.loadClass(REFLECT_PKG_CLASS);
		} catch (ClassNotFoundException cnf) {
			throw new IllegalStateException("Cannot load blueprint classes " + cnf);
		}
	}

	public boolean isTypeCompatible(BundleContext targetContext) {
		Bundle bnd = targetContext.getBundle();
		return (checkCompatibility(CONTAINER_PKG_CLASS, bnd, containerPkgClass) && checkCompatibility(
				REFLECT_PKG_CLASS, bnd, reflectPkgClass));
	}

	private boolean checkCompatibility(String of, Bundle in, Class<?> against) {
		try {
			Class<?> found = in.loadClass(of);
			return against.equals(found);
		} catch (ClassNotFoundException cnf) {
			// no class means compatible
			return true;
		}
	}
}
