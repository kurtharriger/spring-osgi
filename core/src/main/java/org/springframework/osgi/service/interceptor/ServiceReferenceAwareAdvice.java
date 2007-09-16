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
package org.springframework.osgi.service.interceptor;

import java.util.Collections;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.osgi.service.ServiceReferenceAware;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.util.Assert;

/**
 * Mixin implementation for ServiceReferenceAware.
 * 
 * @author Costin Leau
 * 
 */
public class ServiceReferenceAwareAdvice extends DelegatingIntroductionInterceptor implements ServiceReferenceAware {

	private OsgiServiceInvoker serviceInvoker;

	public ServiceReferenceAwareAdvice(OsgiServiceInvoker serviceInvoker) {
		Assert.notNull(serviceInvoker);
		this.serviceInvoker = serviceInvoker;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.ServiceReferenceAware#getServiceProperties()
	 */
	public Map getServiceProperties() {
		ServiceReference ref = getServiceReference();
		return (ref != null ? OsgiServiceReferenceUtils.getServicePropertiesAsMap(ref) : Collections.EMPTY_MAP);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.ServiceReferenceAware#getServiceReference()
	 */
	public ServiceReference getServiceReference() {
		return serviceInvoker.getServiceReference();
	}

	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof ServiceReferenceAwareAdvice) {
			ServiceReferenceAwareAdvice oth = (ServiceReferenceAwareAdvice) other;
			return (serviceInvoker.equals(oth.serviceInvoker));
		}
		else
			return false;
	}
}
