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
package org.springframework.osgi.internal.service.util;

import java.util.Dictionary;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.service.importer.OsgiServiceLifecycleListener;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Costin Leau
 * 
 */
public abstract class OsgiServiceBindingUtils {

	private static final Log log = LogFactory.getLog(OsgiServiceBindingUtils.class);

	public static void callListenersBind(BundleContext context, ServiceReference reference,
			OsgiServiceLifecycleListener[] listeners) {
		if (!ObjectUtils.isEmpty(listeners)) {
			boolean debug = log.isDebugEnabled();
			Object service = OsgiServiceUtils.getService(context, reference);
			// TODO: is snapshot enough or should we use the dynamic lookup?

			// get a Dictionary implementing a Map
			Dictionary properties = OsgiServiceReferenceUtils.getServicePropertiesSnapshot(reference);
			for (int i = 0; i < listeners.length; i++) {
				if (debug)
					log.debug("calling bind on " + listeners[i] + " w/ reference " + reference);
				try {
					listeners[i].bind(service, (Map) properties);
				}
				catch (Exception ex) {
					log.warn("bind method on listener " + listeners[i] + " threw exception ", ex);
				}
			}
		}
	}

	public static void callListenersUnbind(BundleContext context, ServiceReference reference,
			OsgiServiceLifecycleListener[] listeners) {
		if (!ObjectUtils.isEmpty(listeners)) {
			boolean debug = log.isDebugEnabled();
			Object service = OsgiServiceUtils.getService(context, reference);
			// get a Dictionary implementing a Map
			Dictionary properties = OsgiServiceReferenceUtils.getServicePropertiesSnapshot(reference);
			for (int i = 0; i < listeners.length; i++) {
				if (debug)
					log.debug("calling unbind on " + listeners[i] + " w/ reference " + reference);
				try {
					listeners[i].unbind(service, (Map) properties);
				}
				catch (Exception ex) {
					log.warn("unbind method on listener " + listeners[i] + " threw exception ", ex);
				}
			}
		}
	}

}
