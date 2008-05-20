/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.extender.internal.activator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.io.internal.resolver.DependencyResolver;
import org.springframework.osgi.io.internal.resolver.ImportedBundle;
import org.springframework.osgi.io.internal.resolver.PackageAdminResolver;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.util.Assert;

/**
 * Simple utility class for checking the Spring compatibility between two
 * different bundles.
 * 
 * <p/> To escape bundle resolving (which can be difficult as the packages can
 * be spread across multiple bundles), this class simply does class loading and
 * the checks the compatibility between them. Since the bundles are all wired
 * between them, the same Spring version should be used all over.
 * 
 * @author Costin Leau
 * 
 */
class SpringTypeCompatibilityChecker {

	/** Spring class used for loading */
	private static Class SPRING_TYPE = Assert.class;
	/** hold a direct reference since it's a mandatory platform service */
	private final DependencyResolver dependencyResolver;


	SpringTypeCompatibilityChecker(BundleContext bundleContext) {
		// use PackageAdminResolver
		dependencyResolver = new PackageAdminResolver(bundleContext);
	}

	/**
	 * Returns the type compatibility between the given bundle and the Spring
	 * types used by the current version of Spring-DM. If the given bundle does
	 * not import Spring, then true is returned (the bundle is considered
	 * compatible).
	 * 
	 * @param bundle
	 * @return
	 */
	boolean checkCompatibility(Bundle bundle) {
		Assert.notNull(bundle);

		Boolean typeComparison = isTypeAvailable(bundle, SPRING_TYPE);

		if (typeComparison != null) {
			return typeComparison.booleanValue();
		}
		// check the imported bundles
		else {
			ImportedBundle[] importedBundles = dependencyResolver.getImportedBundles(bundle);
			return checkImportedBundles(importedBundles);
		}
	}

	/**
	 * Check the Spring compatibility with the given imported bundles.
	 * 
	 * @param importedBundles
	 * @return
	 */
	private boolean checkImportedBundles(ImportedBundle[] importedBundles) {
		for (int i = 0; i < importedBundles.length; i++) {
			Bundle bundle = importedBundles[i].getBundle();
			// minor optimization (exclude system bundle)
			if (!OsgiBundleUtils.isSystemBundle(bundle)) {
				Boolean comparsion = isTypeAvailable(bundle, SPRING_TYPE);
				if (comparsion != null) {
					return comparsion.booleanValue();
				}
			}
		}
		// no Spring type found, the bundles are compatible
		return true;
	}

	/**
	 * Returns a boolean representing the comparison result if the given type is
	 * found in the given bundle or null otherwise.
	 * 
	 * @param bundle
	 * @param type
	 * @return
	 */
	private static Boolean isTypeAvailable(Bundle bundle, Class type) {
		try {
			Class newType = bundle.loadClass(type.getName());
			return Boolean.valueOf(type.equals(newType));
		}
		catch (ClassNotFoundException cnfe) {
			// class is not available
			return null;
		}
	}

	/**
	 * Utility method used for finding bundles that load a specific type. This
	 * should be used if the PackageAdmin is not available.
	 * 
	 * @param context
	 * @param type
	 * @return
	 */
	static Bundle findOriginatingBundle(BundleContext context, Class type) {
		Bundle[] bundles = context.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			Boolean isAvailable = isTypeAvailable(bundle, type);
			if (isAvailable != null && isAvailable.booleanValue()) {
				return bundle;
			}
		}

		return null;
	}
}
