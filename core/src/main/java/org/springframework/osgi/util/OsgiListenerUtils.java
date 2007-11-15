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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Utility class for dealing with {@link ServiceListener}s. This class contains
 * common functionality such as broadcasting events or safely registering an
 * OSGi listener.
 * 
 * @author Costin Leau
 * 
 */
public abstract class OsgiListenerUtils {

	private static final Log log = LogFactory.getLog(OsgiListenerUtils.class);

	/**
	 * Add a service listener to the given application context, under the
	 * specified filter.
	 * 
	 * @see #addServiceListener(BundleContext, ServiceListener, String)
	 * @param context
	 * @param listener
	 * @param filter
	 */
	public static void addServiceListener(BundleContext context, ServiceListener listener, Filter filter) {
		String toStringFilter = (filter == null ? null : filter.toString());
		addServiceListener(context, listener, toStringFilter);
	}

	/**
	 * Add a service listener to the given application context, under the
	 * specified filter given as a String. The method will also retrieve the
	 * services registered before the listener registration and will inform the
	 * listener through service events of type REGISTERED. This might cause
	 * problems in case a service is being registered between the listener
	 * registration and the retrieval of existing services and thus, can cause
	 * event duplication to occur on the listener.
	 * 
	 * <p/> For most implementations this is not a problem; if it is then do not
	 * use this method.
	 * 
	 * @param context
	 * @param listener
	 * @param filter
	 */
	public static void addServiceListener(BundleContext context, ServiceListener listener, String filter) {
		Assert.notNull(context);
		Assert.notNull(listener);

		try {
			// add listener
			context.addServiceListener(listener, filter);
		}
		catch (InvalidSyntaxException isex) {
			throw (RuntimeException) new IllegalArgumentException("invalid filter").initCause(isex);
		}

		// now get the already registered services and call the listener
		// (the listener can handle duplicates)
		ServiceReference[] alreadyRegistered = OsgiServiceReferenceUtils.getServiceReferences(context, filter);

		if (log.isTraceEnabled())
			log.trace("calling listener on already registered services: "
					+ ObjectUtils.nullSafeToString(alreadyRegistered));

		if (alreadyRegistered != null) {
			for (int i = 0; i < alreadyRegistered.length; i++) {
				listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, alreadyRegistered[i]));
			}
		}
	}

	public static boolean removeServiceListener(BundleContext bundleContext, ServiceListener listener) {
		if (bundleContext == null || listener == null)
			return false;

		try {
			bundleContext.removeServiceListener(listener);
			return true;
		}
		catch (IllegalStateException e) {
			// Bundle context is no longer valid
		}

		return false;
	}
}