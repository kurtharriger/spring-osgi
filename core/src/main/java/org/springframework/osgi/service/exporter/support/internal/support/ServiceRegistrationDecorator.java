/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.service.exporter.support.internal.support;

import java.util.Dictionary;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.util.Assert;

/**
 * Decorator class for {@link ServiceReference} which add notification for {@link ServiceRegistration#unregister()}
 * method when dealing with listeners.
 * 
 * @author Costin Leau
 */
public class ServiceRegistrationDecorator implements ServiceRegistration {

	/** actual service registration */
	private final ServiceRegistration delegate;
	private volatile UnregistrationNotifier notifier;

	public ServiceRegistrationDecorator(ServiceRegistration registration) {
		Assert.notNull(registration);
		this.delegate = registration;
	}

	void setNotifier(UnregistrationNotifier notifier) {
		this.notifier = notifier;
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
		ServiceReference reference = delegate.getReference();

		Map properties =
				(reference != null ? (Map) OsgiServiceReferenceUtils.getServicePropertiesSnapshot(reference) : null);

		if (notifier != null) {
			notifier.unregister(properties);
		}
		delegate.unregister();
	}

	public String toString() {
		return "ServiceRegistrationWrapper for " + delegate.toString();
	}
}