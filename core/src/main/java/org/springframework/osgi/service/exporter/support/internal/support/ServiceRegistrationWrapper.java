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

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.util.Assert;

/**
 * Simple wrapper that prevents a service registration from being unregistered.
 * 
 * @author Costin Leau
 */
public class ServiceRegistrationWrapper implements ServiceRegistration {

	private final ServiceRegistration delegate;

	public ServiceRegistrationWrapper(ServiceRegistration delegate) {
		Assert.notNull(delegate);
		this.delegate = delegate;
	}

	public ServiceReference getReference() {
		return delegate.getReference();
	}

	public void setProperties(Dictionary properties) {
		delegate.setProperties(properties);
	}

	public void unregister() {
		throw new UnsupportedOperationException("Sevice unregistration is not allowed");
	}
}
