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

package org.springframework.osgi.context.internal.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.springframework.osgi.util.OsgiBundleUtils;

/**
 * Utility class used for finding resources and their source bundle inside a
 * bundle class space.
 * 
 * <b>Note:</b> Does not consider split packages
 * 
 * @author Costin Leau
 */
class BundleResourceLocator {

	private static final String CLASS_FILE_SUFFIX = ".class";
	/** logger */
	private static final Log log = LogFactory.getLog(BundleResourceLocator.class);

	private final PackageAdmin pa;
	private final Set<Bundle> bundleNetwork = new LinkedHashSet<Bundle>();
	private final Map<String, Bundle> pkgToBundle = new LinkedHashMap<String, Bundle>();


	BundleResourceLocator(BundleContext bundleContext) {
		this.bundleNetwork.add(bundleContext.getBundle());

		ServiceReference paRef = bundleContext.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin foundPA = null;

		if (paRef != null) {
			foundPA = (PackageAdmin) bundleContext.getService(paRef);
		}
		if (foundPA == null) {
			throw new IllegalArgumentException("PackageAdmin cannot be found");
		}
		this.pa = foundPA;
	}

	InputStream getStreamForClass(String className) {
		// check initial bundle space
		String internalName = className.replace('.', '/') + CLASS_FILE_SUFFIX;
		int idx = className.lastIndexOf('.');
		String pkg = (idx > 0 ? className.substring(0, idx) : "");

		// check cache
		Bundle sourceBundle = pkgToBundle.get(pkg);
		URL resource = null;
		InputStream stream = null;
		if (sourceBundle != null) {
			resource = sourceBundle.getResource(internalName);
			stream = createStreamFrom(resource);

			if (stream != null) {
				return stream;
			}
		}

		Set<Bundle> clone = new LinkedHashSet<Bundle>(bundleNetwork.size());
		clone.addAll(bundleNetwork);

		for (Bundle bundle : clone) {
			resource = findResource(bundle, pkg, internalName);

			if (resource != null) {
				stream = createStreamFrom(resource);
				if (stream != null) {
					return stream;
				}
			}
		}
		return stream;
	}

	private URL findResource(Bundle bundle, String pkg, String internalName) {
		URL resource = bundle.getResource(internalName);

		// find wiring
		if (resource != null) {

			// find source bundle
			if (!OsgiBundleUtils.isSystemBundle(bundle)) {
				ExportedPackage[] exportedPackages = pa.getExportedPackages(pkg);
				if (exportedPackages != null) {
					for (ExportedPackage exportedPackage : exportedPackages) {
						if (!exportedPackage.isRemovalPending()) {
							Bundle[] importingBundles = exportedPackage.getImportingBundles();
							if (importingBundles != null) {
								for (Bundle importingBundle : importingBundles) {
									if (importingBundle.equals(bundle)) {
										Bundle exportingBundle = exportedPackage.getExportingBundle();
										bundleNetwork.add(exportingBundle);
										pkgToBundle.put(pkg, exportingBundle);
										return resource;
									}
								}
							}
						}
					}
				}
			}
			pkgToBundle.put(pkg, bundle);
		}

		// no wiring, the resource is provided by the bundle itself
		return resource;
	}

	private InputStream createStreamFrom(URL resource) {
		if (resource != null) {
			try {
				URLConnection conn = resource.openConnection();
				conn.setUseCaches(false);
				return conn.getInputStream();
			}
			catch (IOException ex) {
				log.warn("Cannot open stream to resource " + resource, ex);
				// keep searching
			}
		}
		return null;
	}
}