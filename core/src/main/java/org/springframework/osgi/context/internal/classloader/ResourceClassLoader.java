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

import java.io.InputStream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.core.OverridingClassLoader;
import org.springframework.util.StringUtils;

/**
 * OSGi specific class loader, used for loading classes by doing resource
 * lookups in the OSGi space.
 * 
 * @author Costin Leau
 */
public class ResourceClassLoader extends OverridingClassLoader {

	private final BundleResourceLocator resourceLocator;
	/** boot delegated packages */
	private final String[] bootDelegatedPackages;


	public ResourceClassLoader(ClassLoader parent, BundleContext bundleContext) {
		super(parent);

		this.resourceLocator = new BundleResourceLocator(bundleContext);
		this.bootDelegatedPackages = initBootDelegatedPackages(bundleContext);
	}

	private String[] initBootDelegatedPackages(BundleContext context) {
		String bootDelegationString = StringUtils.trimWhitespace(context.getProperty(Constants.FRAMEWORK_BOOTDELEGATION));
		if (!StringUtils.hasText(bootDelegationString)) {
			return null;
		}

		String[] pkgs = StringUtils.commaDelimitedListToStringArray(bootDelegationString);
		for (int i = 0; i < pkgs.length; i++) {
			pkgs[i] = pkgs[i].trim();
			if (pkgs[i].endsWith("*")) {
				pkgs[i] = pkgs[i].substring(0, pkgs[i].length() - 1);
			}
		}
		return pkgs;
	}

	@Override
	protected boolean isEligibleForOverriding(String className) {
		// check bootstrap packages
		if (isBootDelegated(className)) {
			return false;
		}

		return true;
	}

	private boolean isBootDelegated(String className) {
		if (className.startsWith("java.")) {
			return true;
		}

		String pkg = getPackageName(className);

		for (String bootPkg : bootDelegatedPackages) {
			if (bootPkg.equals(pkg) || pkg.startsWith(bootPkg)) {
				return true;
			}
		}
		return false;
	}

	private String getPackageName(String className) {
		int idx = className.lastIndexOf('.');
		return (idx > 0 ? className.substring(0, idx) : "");
	}

	@Override
	protected InputStream openStreamForClass(String name) {
		return resourceLocator.getStreamForClass(name);
	}

	@Override
	public void excludeClass(String className) {
		super.excludeClass(className);
		new Exception("Excluding class " + className + " trigged by ").printStackTrace();
	}

	@Override
	public void excludePackage(String packageName) {
		super.excludePackage(packageName);
		System.out.println("Excluding package " + packageName);
	}
}