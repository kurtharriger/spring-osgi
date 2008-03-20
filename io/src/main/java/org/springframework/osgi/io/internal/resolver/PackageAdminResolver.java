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

package org.springframework.osgi.io.internal.resolver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.springframework.osgi.io.internal.OsgiResourceUtils;
import org.springframework.util.Assert;

/**
 * {@link PackageAdmin} based dependency resolver.
 * 
 * <p/> This implementation uses the OSGi PackageAdmin service to determine
 * dependencies between bundles. Since it's highly dependent on an external
 * service, it might be better to use a listener based implementation for
 * non-performant environments.
 * 
 * <p/> This implementation does consider required bundles.
 * 
 * @author Costin Leau
 * 
 */
public class PackageAdminResolver implements DependencyResolver {

	private final BundleContext bundleContext;


	public PackageAdminResolver(BundleContext bundleContext) {
		Assert.notNull(bundleContext);
		this.bundleContext = bundleContext;
	}

	public ImportedBundle[] getImportedBundles(Bundle bundle) {
		PackageAdmin pa = getPackageAdmin();

		// create map with bundles as keys and a list of packages as value
		Map importedBundles = new LinkedHashMap(8);

		// 1. consider required bundles first

		// see if there are required bundle(s) defined
		String[] entries = OsgiResourceUtils.getRequiredBundle(bundle);

		// 1. if so, locate the bundles
		for (int i = 0; i < entries.length; i++) {
			String version = "0.0.0";
			// determine the bundle version
			String entry = entries[i];
			int index = entry.indexOf(Constants.BUNDLE_VERSION_ATTRIBUTE);
			if (index > 0) {
				// find quotes
				int firstQuoteIndex = entry.indexOf("\"", index);
				int secondQuoteIndex = entry.indexOf("\"", firstQuoteIndex);
				version = entry.substring(firstQuoteIndex, secondQuoteIndex + 1);
			}

			Bundle requiredBundle = pa.getBundles(entries[i], version)[0];

			// find exported packages
			ExportedPackage[] exportedPackages = pa.getExportedPackages(requiredBundle);
			addExportedPackages(importedBundles, bundle, exportedPackages);
		}

		// 2. determine imported bundles 
		// get all bundles
		Bundle[] bundles = bundleContext.getBundles();

		for (int i = 0; i < bundles.length; i++) {
			Bundle analyzedBundle = bundles[i];
			// if the bundle is already included (it's a required one), there's no need to look at it again
			if (!importedBundles.containsKey(analyzedBundle)) {
				ExportedPackage[] epa = pa.getExportedPackages(analyzedBundle);
				if (epa != null)
					for (int j = 0; j < epa.length; j++) {
						ExportedPackage exportedPackage = epa[j];
						Bundle[] importingBundles = exportedPackage.getImportingBundles();
						if (importingBundles != null)
							for (int k = 0; k < importingBundles.length; k++) {
								if (bundle.equals(importingBundles[k])) {
									addImportedBundle(importedBundles, exportedPackage.getExportingBundle(),
										exportedPackage.getName());
								}
							}
					}
			}
		}

		List importedBundlesList = new ArrayList(importedBundles.size());

		for (Iterator iterator = importedBundles.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			Bundle importedBundle = (Bundle) entry.getKey();
			List packages = (List) entry.getValue();
			importedBundlesList.add(new ImportedBundle(importedBundle,
				(String[]) packages.toArray(new String[packages.size()])));
		}

		return (ImportedBundle[]) importedBundlesList.toArray(new ImportedBundle[importedBundlesList.size()]);
	}

	/**
	 * Adds the imported bundles to the map of packages.
	 * 
	 * @param map
	 * @param bundle
	 * @param packageName
	 */
	private void addImportedBundle(Map map, Bundle bundle, String packageName) {
		List packages = (List) map.get(bundle);
		if (packages == null) {
			packages = new ArrayList(4);
			map.put(bundle, packages);
		}
		packages.add(packageName);
	}

	private void addExportedPackages(Map map, Bundle bundle, ExportedPackage[] pkgs) {
		List packages = (List) map.get(bundle);
		if (packages == null) {
			packages = new ArrayList(pkgs.length);
			map.put(bundle, packages);
		}
		for (int i = 0; i < pkgs.length; i++) {
			ExportedPackage exportedPackage = pkgs[i];
			packages.add(exportedPackage.getName());
		}
	}

	private PackageAdmin getPackageAdmin() {
		ServiceReference ref = bundleContext.getServiceReference(PackageAdmin.class.getName());
		if (ref == null)
			throw new IllegalStateException(PackageAdmin.class.getName() + " service is required");
		// don't do any proxying since PackageAdmin is normally a framework service
		// we can assume for now that it will stay
		return (PackageAdmin) bundleContext.getService(ref);
	}
}
