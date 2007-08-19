/*
 * Copyright 2002-2006 the original author or authors.
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
 *
 */
package org.springframework.osgi.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.util.Assert;

/**
 * Regrettably we sometimes need to know which Osgi platform we are currently
 * running on in order to work around quirks and differences in the different
 * implementations.
 * 
 * Currently we can detect equinox, knopflerfish, and felix.
 * 
 * @author Adrian Colyer
 */
public abstract class OsgiPlatformDetector {

    private static final String[] EQUINOX_LABELS = new String[] { "Eclipse", "eclipse", "Equinox", "equinox", };

	private static final String[] KF_LABELS = new String[] { "Knopflerfish", "knopflerfish" };

	private static final String[] FELIX_LABELS = new String[] { "Apache Software Foundation", "Felix", "felix" };


    public static boolean isEquinox(BundleContext aContext) {
		return determinePlatform(aContext, EQUINOX_LABELS);
	}

	public static boolean isKnopflerfish(BundleContext aContext) {
		return determinePlatform(aContext, KF_LABELS);
	}

	public static boolean isFelix(BundleContext aContext) {
		return determinePlatform(aContext, FELIX_LABELS);
	}

    private static boolean determinePlatform(BundleContext context, String[] labels) {
		Assert.notNull(context);
		Assert.notNull(labels);

		String vendorProperty = context.getProperty(Constants.FRAMEWORK_VENDOR);
		if (vendorProperty == null) {
			return false; // might be running outside of container
		}
		else {
			// code defensively here to allow for variation in vendor name over
			// time
			if (containsAnyOf(vendorProperty, labels)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsAnyOf(String source, String[] searchTerms) {
		for (int i = 0; i < searchTerms.length; i++) {
			if (source.indexOf(searchTerms[i]) != -1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the OSGi platform version (using the manifest entries from the
	 * system bundle). Can be null or empty.
	 * 
	 * Subclasses should extend this if a different detection mechanism is
	 * required.
	 * 
	 * @param ctx bundle context to inspect
	 * @return system bundle version.
	 */
	public static String getVersion(BundleContext ctx) {
		if (ctx == null)
			return "";

		// get system bundle
		Bundle sysBundle = ctx.getBundle(0);
		// force string conversion instead of casting just to be safe
		return "" + sysBundle.getHeaders().get(Constants.BUNDLE_VERSION);
	}

}