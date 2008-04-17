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

	private static final String SEMI_COLON = ";";
	private static final String DOUBLE_QUOTE = "\"";
	private static final String DEFAULT_VERSION = "0.0.0";
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
		String[] entries = OsgiResourceUtils.getRequireBundle(bundle);

		// 1. if so, locate the bundles
		for (int i = 0; i < entries.length; i++) {
			String[] parsed = parseRequiredBundleString(entries[i]);
			Bundle requiredBundle = pa.getBundles(parsed[0], parsed[1])[0];

			// find exported packages
			ExportedPackage[] exportedPackages = pa.getExportedPackages(requiredBundle);
			addExportedPackages(importedBundles, requiredBundle, exportedPackages);
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
	 * Parses the required bundle entry to determine the bundle symbolic name
	 * and version.
	 * 
	 * @param string required bundle entry
	 * @return returns an array of strings with 2 entries, the first being the
	 * bundle sym name, the second the version (or 0.0.0 if nothing is
	 * specified).
	 */
	String[] parseRequiredBundleString(String entry) {
		String[] value = new String[2];

		// determine the bundle symbolic name
		int index = entry.indexOf(SEMI_COLON);

		// there is at least one flag so extract the sym name
		if (index > 0) {
			value[0] = entry.substring(0, index);
		}
		// no flag, short circuit
		else {
			value[0] = entry;
			value[1] = DEFAULT_VERSION;
			return value;
		}

		// look for bundle-version
		index = entry.indexOf(Constants.BUNDLE_VERSION_ATTRIBUTE);
		if (index > 0) {
			// skip the =
			int firstQuoteIndex = index + Constants.BUNDLE_VERSION_ATTRIBUTE.length() + 1;
			// check if a range or a number is specified
			char testChar = entry.charAt(firstQuoteIndex + 1);
			boolean isRange = (testChar == '[' || testChar == '(');
			int secondQuoteStartIndex = (isRange ? firstQuoteIndex + 3 : firstQuoteIndex + 1);

			int numberStart = (isRange ? firstQuoteIndex + 3 : firstQuoteIndex + 1);
			int numberEnd = entry.indexOf(DOUBLE_QUOTE, secondQuoteStartIndex);

			value[1] = entry.substring(numberStart, numberEnd);
			// if it's a range, append the interval notation
			if (isRange) {
				value[1] = entry.charAt(firstQuoteIndex + 1) + value[1] + entry.charAt(numberEnd + 1);
			}
		}
		else {
			value[1] = DEFAULT_VERSION;
		}

		return value;
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

	/**
	 * Adds the bundle exporting the given packages which are then imported by
	 * the owning bundle. This applies to special imports (such as
	 * Require-Bundle).
	 * 
	 * @param map
	 * @param bundle
	 * @param pkgs
	 */
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
};
