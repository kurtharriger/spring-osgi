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
package org.springframework.osgi.service.importer.support.internal.aop;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;

/**
 * Container class encapsulating ServiceReference specific operations. Note that this class is highly tied to the
 * behaviour of {@link ServiceDynamicInterceptor} and should not be used elsewhere.
 * 
 * @author Costin Leau
 */
class ReferenceHolder {
	private final ServiceReference reference;
	private final BundleContext bundleContext;
	private final long id;
	private final int ranking;

	private volatile Object service;

	public ReferenceHolder(ServiceReference reference, BundleContext bundleContext) {
		this.reference = reference;
		this.bundleContext = bundleContext;
		id = OsgiServiceReferenceUtils.getServiceId(reference);
		ranking = OsgiServiceReferenceUtils.getServiceRanking(reference);
	}

	public Object getService() {
		if (service != null) {
			return service;
		}
		if (reference != null) {
			service = bundleContext.getService(reference);
			return service;
		}

		return null;
	}

	public long getId() {
		return id;
	}

	public int getRanking() {
		return ranking;
	}

	public ServiceReference getReference() {
		return reference;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof ReferenceHolder) {
			return ((ReferenceHolder) obj).id == id;
		}

		if (obj instanceof ServiceReference) {
			return id == OsgiServiceReferenceUtils.getServiceId(reference);
		}

		return false;
	}

	public boolean isWorseThen(ServiceReference ref) {
		int otherRanking = OsgiServiceReferenceUtils.getServiceRanking(ref);
		// if there is a higher ranking service
		if (otherRanking > ranking) {
			return true;
		}
		// if equal, use the service id
		if (otherRanking == ranking) {
			long otherId = OsgiServiceReferenceUtils.getServiceId(ref);
			if (otherId < id) {
				return true;
			}
		}
		return false;
	}
}