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

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.core.ConstantException;
import org.springframework.core.Constants;
import org.springframework.osgi.AmbiguousServiceReferenceException;
import org.springframework.osgi.NoSuchServiceException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Utility class offering easy access to OSGi services
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
public abstract class OsgiServiceUtils {

	public static final Constants BUNDLE_EVENTS = new Constants(BundleEvent.class);

	/**
	 * Find the single OSGi service of the given type and matching the given
	 * filter. Throws an NoSuchServiceException if there are no matching
	 * services, or AmbiguousServiceReferenceException if there are more than
	 * one candidate matches.
	 * 
	 * @param context
	 * @param serviceClass
	 * @param filter
	 * @return the service of the given type matching the given filter
	 * @throws NoSuchServiceException if a matching service cannot be found
	 * @throws AmbiguousServiceReferenceException if multiple matching services
	 * are found
	 * @throws IllegalArgumentException if the filter string is non-null and is
	 * not well-formed
	 */
	public static ServiceReference getService(BundleContext context, Class serviceClass, String filter)
			throws NoSuchServiceException, AmbiguousServiceReferenceException, IllegalArgumentException {
		Assert.notNull(context, "context cannot be null");
		Assert.notNull(serviceClass, "serviceClass cannot be null");
		try {
			ServiceReference[] serviceReferences = context.getServiceReferences(serviceClass.getName(), filter);
			if (serviceReferences == null || serviceReferences.length == 0) {
				throw new NoSuchServiceException("A service of type '" + serviceClass.getName() + "' matching filter '"
						+ ((filter == null) ? "" : filter) + "' could not be found.", serviceClass, filter);
			}
			else if (serviceReferences.length > 1) {
				throw new AmbiguousServiceReferenceException("Found " + serviceReferences.length
						+ " services of type '" + serviceClass.getName() + "' matching filter '"
						+ ((filter == null) ? "" : filter) + "' (expecting only one)", serviceClass, filter);
			}
			else {
				return serviceReferences[0];
			}
		}
		catch (InvalidSyntaxException ex) {
			throw (IllegalArgumentException) new IllegalArgumentException(ex.getMessage()).initCause(ex);
		}
	}

	/**
	 * Return all of the service references for services of the given type and
	 * matching the given filter. Returned service references must be compatible
	 * with the given context.
	 * 
	 * @param context
	 * @param serviceClass
	 * @param filter
	 * @throws IllegalArgumentException if the filter string is non-null and is
	 * not well-formed
	 */
	public static ServiceReference[] getServices(BundleContext context, Class serviceClass, String filter)
			throws IllegalArgumentException {
		try {
			ServiceReference[] serviceReferences = context.getServiceReferences(serviceClass.getName(), filter);
			return serviceReferences;
		}
		catch (InvalidSyntaxException ex) {
			throw (IllegalArgumentException) new IllegalArgumentException(ex.getMessage()).initCause(ex);
		}
	}

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

	public static Object getService(BundleContext context, ServiceReference reference) {
		Assert.notNull(context);
		Assert.notNull(reference);

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
	 * @param registration
	 * @return
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

	/**
	 * Convert event codes to a printable String.
	 * 
	 * @param type
	 */
	public static String getBundleEventAsString(int bundleEvent) {
		try {
			return BUNDLE_EVENTS.toCode(new Integer(bundleEvent), "");
		}
		catch (ConstantException cex) {
			return "UNKNOWN EVENT TYPE";
		}
	}

	/**
	 * Parse the given class array and eliminate parents of existing classes.
	 * 
	 * @param classes
	 * @return
	 */
	public static Class[] removeParents(Class[] classes) {
		if (ObjectUtils.isEmpty(classes))
			return new Class[0];

		List clazz = new ArrayList(classes.length);
		for (int i = 0; i < classes.length; i++) {
			clazz.add(classes[i]);
		}

		// remove null elements
		while (clazz.remove(null)) {
		}

		// only one class is allowed
		// there can be multiple interfaces
		// parents of classes inside the array are removed
		// it will be removed

		boolean dirty;
		do {
			dirty = false;
			for (int i = 0; i < clazz.size(); i++) {
				Class currentClass = (Class) clazz.get(i);
				for (int j = 0; j < clazz.size(); j++) {
					if (i != j) {
						if (currentClass.isAssignableFrom((Class) clazz.get(j))) {
							clazz.remove(i);
							i--;
							dirty = true;
							break;
						}
					}
				}
			}
		} while (dirty);

		return (Class[]) clazz.toArray(new Class[clazz.size()]);
	}

}
