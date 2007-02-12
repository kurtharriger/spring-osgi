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
package org.springframework.osgi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Simple utility class for broadcasting events to various listeners.
 * 
 * @author Costin Leau
 * 
 */
public abstract class OsgiBindingUtils {

	private static final Log log = LogFactory.getLog(OsgiBindingUtils.class);

	public static void callListenersBind(BundleContext context, ServiceReference reference,
			TargetSourceLifecycleListener[] listeners) {
		boolean debug = log.isDebugEnabled();
		Object service = OsgiServiceUtils.getService(context, reference);
		for (int i = 0; i < listeners.length; i++) {
			{
				if (debug)
					log.debug("calling bind on " + listeners[i] + " w/ reference " + reference);
				try {
					listeners[i].bind(service, OsgiServiceReferenceUtils.getServiceProperties(reference));
				}
				catch (Exception ex) {
					log.warn("bind method on listener " + listeners[i] + " threw exception ", ex);
				}
			}
		}
	}

	public static void callListenersUnbind(BundleContext context, ServiceReference reference,
			TargetSourceLifecycleListener[] listeners) {
		boolean debug = log.isDebugEnabled();
		Object service = OsgiServiceUtils.getService(context, reference);
		for (int i = 0; i < listeners.length; i++) {
			if (debug)
				log.debug("calling unbind on " + listeners[i] + " w/ reference " + reference);
			try {
				listeners[i].unbind(service, OsgiServiceReferenceUtils.getServiceProperties(reference));
			}
			catch (Exception ex) {
				log.warn("unbind method on listener " + listeners[i] + " threw exception ", ex);
			}
		}
	}
}
