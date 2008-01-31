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

package org.springframework.osgi.io.internal;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.springframework.util.Assert;

/**
 * {@link PackageAdmin} based dependency resolver.
 * 
 * <p>
 * This implementation uses the OSGi PackageAdmin service to determine
 * dependencies between bundles. Since it's highly dependent on an external
 * service, it might be better to use a listener based implementation for
 * non-performant environments.
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

	public Bundle[] getImportedBundle(Bundle bundle) {
		PackageAdmin pa = getPackageAdmin();

		// get all bundles
		Bundle[] bundles = bundleContext.getBundles();

		List importedBundles = new ArrayList(8);

		// first add required bundles
		RequiredBundle[] requiredBundles = pa.getRequiredBundles(bundle.getSymbolicName());
		if (requiredBundles != null)
			for (int i = 0; i < requiredBundles.length; i++) {
				RequiredBundle requiredBundle = requiredBundles[i];
				importedBundles.add(requiredBundle.getBundle());
			}

		// exclude framework bundle?
		for (int i = 0; i < bundles.length; i++) {
			Bundle analyzedBundle = bundles[i];
			// if the bundle is already included, there's no need to look at it again
			if (!importedBundles.contains(analyzedBundle)) {
				ExportedPackage[] epa = pa.getExportedPackages(analyzedBundle);
				if (epa != null)
					for (int j = 0; j < epa.length; j++) {
						Bundle[] importingBundles = epa[j].getImportingBundles();
						if (importingBundles != null)
							for (int k = 0; k < importingBundles.length; k++) {
								if (bundle.equals(importingBundles[k])) {
									importedBundles.add(analyzedBundle);
								}
							}
					}
			}
		}

		return (Bundle[]) importedBundles.toArray(new Bundle[importedBundles.size()]);
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
