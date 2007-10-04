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

import java.util.Dictionary;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.internal.service.util.OsgiServiceRegistrationListener;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.util.Assert;

/**
 * Wrapper class for {@link ServiceReference} which add notification for
 * {@link ServiceRegistration#unregister()} method when dealing with listeners.
 * 
 * @author Costin Leau
 */
public class ServiceRegistrationWrapper implements ServiceRegistration {

	/** actual service registration */
	private final ServiceRegistration delegate;

	private final OsgiServiceRegistrationListener[] listeners;

	public ServiceRegistrationWrapper(ServiceRegistration registration, OsgiServiceRegistrationListener[] listeners) {
		Assert.notNull(registration);
		this.delegate = registration;
		this.listeners = (listeners == null ? new OsgiServiceRegistrationListener[0] : listeners);
	}

	public ServiceReference getReference() {
		return delegate.getReference();
	}

	public void setProperties(Dictionary properties) {
		delegate.setProperties(properties);
	}

	// call unregister on the actual service but inform also listeners
	public void unregister() {
		// if the delegate is unregistered then an exception will be thrown
		Map properties = OsgiServiceReferenceUtils.getServicePropertiesSnapshotAsMap(delegate.getReference());

		delegate.unregister();

		// if no exception has been thrown (i.e. the delegate is properly
		// unregistered), the liteners will be informed
		for (int i = 0; i < listeners.length; i++) {
			if (listeners[i] != null)
				listeners[i].unregistered(properties);
		}
	}

	public String toString() {
		return "ServiceWrapper for " + delegate.toString();
	}

}
