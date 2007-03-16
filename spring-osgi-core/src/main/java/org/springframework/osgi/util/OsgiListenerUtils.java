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
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.util.Assert;

/**
 * Utility class for various types of listeners used inside Spring/OSGi. This
 * class contains common functionality such as broadcasting events or safely
 * registering an OSGi {@link ServiceListener}.
 * 
 * @author Costin Leau
 * 
 */
public abstract class OsgiListenerUtils {


	public static void addServiceListener(BundleContext context, ServiceListener listener, Filter filter) {
		String toStringFilter = (filter == null ? null : filter.toString());
		addServiceListener(context, listener, toStringFilter);
	}

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

		if (alreadyRegistered != null) {
			for (int i = 0; i < alreadyRegistered.length; i++) {
				listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, alreadyRegistered[i]));
			}
		}
	}
}
