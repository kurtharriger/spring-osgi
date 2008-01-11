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
 * Created on 25-Jan-2006 by Adrian Colyer
 */

package org.springframework.osgi.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.util.Assert;

/**
 * Utility class offering easy access to OSGi services.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
// FIXME: removed deprecated methods
public abstract class OsgiServiceUtils {

	/**
	 * Return all of the service references for services of the given type and
	 * matching the given filter. Returned services may use interface versions
	 * that are incompatible with the given context.
	 * 
	 * @param context
	 * @param serviceClass
	 * @param filter
	 * @throws IllegalArgumentException if the filter string is non-null and is
	 * not well-formed
	 */
	/**
	 * @deprecated - will be removed in 1.0 final.
	 */
	public static ServiceReference[] getAllServices(BundleContext context, Class serviceClass, String filter)
			throws IllegalArgumentException {
		try {
			ServiceReference[] serviceReferences = context.getAllServiceReferences(serviceClass.getName(), filter);
			return serviceReferences;
		}
		catch (InvalidSyntaxException ex) {
			throw (IllegalArgumentException) new IllegalArgumentException(ex.getMessage()).initCause(ex);
		}
	}

	/**
	 * @deprecated - will be removed in 1.0 final.
	 */
	public static Object getService(BundleContext context, ServiceReference reference) {
		Assert.notNull(context);

		if (reference == null)
			return null;

		try {
			return context.getService(reference);
		}
		finally {
			try {
				// context.ungetService(reference);
			}
			catch (IllegalStateException isex) {
				// do nothing
			}
		}
	}

	/**
	 * Unregisters the given service registration from the given bundle. Returns
	 * true if the unregistration process succeeded, false otherwise.
	 * 
	 * @param registration service registration (can be null)
	 * @return true if the unregistration successeded, false otherwise
	 */
	public static boolean unregisterService(ServiceRegistration registration) {
		try {
			if (registration != null) {
				registration.unregister();
				return true;
			}
		}
		catch (IllegalStateException alreadyUnregisteredException) {
		}
		return false;
	}

}
