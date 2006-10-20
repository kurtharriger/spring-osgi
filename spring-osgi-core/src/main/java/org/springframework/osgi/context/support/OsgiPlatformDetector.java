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
package org.springframework.osgi.context.support;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Regrettably we sometimes need to know which Osgi platform we are
 * currently running on in order to work around quirks and differences
 * in the different implementations.
 * 
 * Currently we can detect equinox, knopflerfish, and felix.
 * 
 * @author Adrian Colyer
 */
public class OsgiPlatformDetector {
	
	private OsgiPlatformDetector() {};
	
	public static boolean isEquinox(BundleContext aContext) {
		String vendorProperty = aContext.getProperty(Constants.FRAMEWORK_VENDOR);
		if (vendorProperty == null) {
			return false;  // might be running outside of container
		}
		else {
			// code defensively here to allow for variation in vendor name over time
			// current implementation of equinox returns the first search term
			if (containsAnyOf(vendorProperty,new String[] {"Eclipse","eclipse","Equinox","equinox",})) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isKnopflerfish(BundleContext aContext) {
		String vendorProperty = aContext.getProperty(Constants.FRAMEWORK_VENDOR);
		if (vendorProperty == null) {
			return false;  // might be running outside of container
		}
		else {
			// code defensively here to allow for variation in vendor name over time
			// current implementation of Knopflerfish returns the first search term
			if (containsAnyOf(vendorProperty,new String[] {"Knopflerfish","knopflerfish"})) {
				return true;
			}
		}
		return false;		
	}
	
	public static boolean isFelix(BundleContext aContext) {
		String vendorProperty = aContext.getProperty(Constants.FRAMEWORK_VENDOR);
		if (vendorProperty == null) {
			return false;  // might be running outside of container
		}
		else {
			// code defensively here to allow for variation in vendor name over time
			// current implementation of Felix returns the first search term...
			if (containsAnyOf(vendorProperty,new String[] {"Apache Software Foundation","Felix","felix"})) {
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
	
}