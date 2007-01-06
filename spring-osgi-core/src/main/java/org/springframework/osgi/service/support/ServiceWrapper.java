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
package org.springframework.osgi.service.support;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.util.Assert;

/**
 * Wrapper around a service reference offering identity and equality.
 * 
 * @author Costin Leau
 * 
 */
public class ServiceWrapper {

	private final ServiceReference reference;

	private final long serviceId;

	private final String toString;

	/** this should be determined in OSGi 4.1 directly from the Bundle * */
	private final BundleContext context;

	public ServiceWrapper(ServiceReference ref, BundleContext bundleContext) {
		Assert.notNull(ref, "not null service reference required");
		Assert.notNull(bundleContext, "bundleContext required");

		reference = ref;
		this.context = bundleContext;

		serviceId = ((Long) ref.getProperty(Constants.SERVICE_ID)).longValue();
		toString = "ServiceWrapper[serviceId=" + serviceId + "|ref=" + reference + "]";
	}

	public boolean isServiceAlive() {
		return (reference.getBundle() != null);
	}

	public boolean equals(Object obj) {
		if (obj instanceof ServiceWrapper) {
			return (reference == ((ServiceWrapper) obj).reference);
		}
		return false;
	}

	public int hashCode() {
		return (int) serviceId;
	}

	public String toString() {
		return toString;
	}

	public Object getService() {
		// TODO: check synch and service cleanup
		synchronized (reference) {
			if (isServiceAlive()) {
				try {
					return context.getService(reference);
				}
				finally {
					context.ungetService(reference);
				}
			}
		}
		return null;
	}

	public ServiceReference getReference() {
		return reference;
	}

	public long getServiceId() {
		return serviceId;
	}

}
