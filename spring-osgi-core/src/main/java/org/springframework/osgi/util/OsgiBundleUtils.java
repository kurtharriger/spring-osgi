/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.ConstantException;
import org.springframework.core.Constants;
import org.springframework.util.Assert;

/**
 * Utility class for OSGi {@link Bundle}s.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 * 
 */
public abstract class OsgiBundleUtils {

	public static final Constants BUNDLE_STATES = new Constants(Bundle.class);

	/**
	 * Return the underlying BundleContext for the given Bundle. This uses
	 * reflection and highly dependent of the OSGi implementation. Should be
	 * removed if OSGi 4.1 is being used.
	 * 
	 * @param bundle
	 * @return
	 */
	public static BundleContext getBundleContext(Bundle bundle) {
		if (bundle == null)
			return null;
		try {
			// Retrieve bundle context from Equinox
			Method m = bundle.getClass().getDeclaredMethod("getContext", new Class[0]);
			m.setAccessible(true);
			return (BundleContext) m.invoke(bundle, new Object[0]);
		}
		catch (Throwable t) {
			// Retrieve bundle context from Knopflerfish
			try {
				Field[] fields = bundle.getClass().getDeclaredFields();
				Field f = null;
				for (int i = 0; i < fields.length; i++) {
					if (fields[i].getName().equals("bundleContext")) {
						f = fields[i];
						break;
					}
				}
				if (f == null) {
					throw new IllegalStateException("No bundleContext field!");
				}
				f.setAccessible(true);
				return (BundleContext) f.get(bundle);
			}
			catch (IllegalAccessException e) {
				throw (IllegalStateException) new IllegalStateException("Exception retrieving bundle context").initCause(e);
			}
		}
	}

	/**
	 * Return true if the given bundle is active or not.
	 * 
	 * @param bundle
	 * @return
	 */
	public static boolean isBundleActive(Bundle bundle) {
		Assert.notNull(bundle, "bundle is required");
		int state = bundle.getState();
		return (state == Bundle.ACTIVE || state == Bundle.STARTING);
	}

	/**
	 * Return the Bundle state as a String.
	 * 
	 * @param bundle
	 * @return
	 */
	public static String getBundleStateAsString(Bundle bundle) {
		Assert.notNull(bundle, "bundle is required");
		int state = bundle.getState();

		try {
			return BUNDLE_STATES.toCode(new Integer(state), "");
		}
		catch (ConstantException cex) {
			return "UNKNOWN STATE";
		}
	}

	public static String getNullSafeSymbolicName(Bundle bundle) {
		Assert.notNull(bundle, "bundle is required");
		return (String) (bundle.getSymbolicName() == null ? bundle.getHeaders().get(
			org.osgi.framework.Constants.BUNDLE_NAME) : bundle.getSymbolicName());
	}

}
