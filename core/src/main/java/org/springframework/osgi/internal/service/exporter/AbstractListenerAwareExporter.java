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
package org.springframework.osgi.internal.service.exporter;

import java.util.Map;

import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.internal.service.ServiceExporter;
import org.springframework.osgi.service.OsgiServiceRegistrationListener;

/**
 * {@link ServiceExporter} extension that takes care of listeners registration
 * and notification.
 * 
 * @author Costin Leau
 */
public abstract class AbstractListenerAwareExporter extends AbstractServiceExporter {

	private OsgiServiceRegistrationListener[] listeners = new OsgiServiceRegistrationListener[0];

	/**
	 * Take care of notifying the listeners on both startup and shutdown (by
	 * wrapping with a special service registration).
	 * 
	 * @param properties
	 * @return
	 */
	protected ServiceRegistration notifyListeners(Map properties, ServiceRegistration registration) {
		// notify listeners
		callRegisteredOnListeners(properties);
		// wrap registration to be notified of unregistration
		return new ServiceRegistrationWrapper(registration, listeners);
	}

	/**
	 * Call registration on listeners.
	 * 
	 * @param properties
	 */
	private void callRegisteredOnListeners(Map properties) {
		for (int i = 0; i < listeners.length; i++) {
			if (listeners[i] != null) {
				try {
					listeners[i].registered(properties);
				}
				catch (Exception ex) {
					// no need to log exceptions, the wrapper already does this
				}
			}

		}
	}

	public void setListeners(OsgiServiceRegistrationListener[] listeners) {
		if (listeners != null)
			this.listeners = listeners;
	}

}
