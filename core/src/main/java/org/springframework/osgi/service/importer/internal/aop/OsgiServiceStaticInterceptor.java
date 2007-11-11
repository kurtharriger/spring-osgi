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
 */
package org.springframework.osgi.service.importer.internal.aop;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.service.ServiceUnavailableException;
import org.springframework.util.Assert;

/**
 * Interceptor offering static behaviour around an OSGi service. If the OSGi
 * becomes unavailable, no look up or retries will be executed, the interceptor
 * throwing an exception.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceStaticInterceptor extends OsgiServiceInvoker {

	private final ServiceReference reference;

	private final BundleContext bundleContext;

	public OsgiServiceStaticInterceptor(BundleContext context, ServiceReference reference) {
		Assert.notNull(context);
		Assert.notNull(reference, "a not null service reference is required");
		this.bundleContext = context;
		this.reference = reference;
	}

	protected Object getTarget() {
		// check if the service is alive first
		if (reference.getBundle() != null) {
			// since requesting for a service requires additional work
			// from the OSGi platform
			Object target = bundleContext.getService(reference);
			if (target != null)
				return target;
		}
		// throw exception
		throw new ServiceUnavailableException(reference);
	}

	public ServiceReference getServiceReference() {
		return reference;
	}

	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof OsgiServiceStaticInterceptor) {
			OsgiServiceStaticInterceptor oth = (OsgiServiceStaticInterceptor) other;
			return reference.equals(oth.reference) && bundleContext.equals(bundleContext);
		}
		return false;
	}
}
