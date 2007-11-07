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
package org.springframework.osgi.internal.service.interceptor;

import org.osgi.framework.ServiceReference;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.osgi.service.importer.ServiceReferenceAccessor;
import org.springframework.util.Assert;

/**
 * Mix-in implementation for ServiceReferenceAccessor.
 * 
 * @author Costin Leau
 * 
 */
public class ServiceReferenceAwareAdvice extends DelegatingIntroductionInterceptor implements ServiceReferenceAccessor {

	private ServiceReference reference;

	public ServiceReferenceAwareAdvice(ServiceReference reference) {
		Assert.notNull(reference);
		this.reference = reference;
	}

	public ServiceReference getServiceReference() {
		return reference;
	}

	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof ServiceReferenceAwareAdvice) {
			ServiceReferenceAwareAdvice oth = (ServiceReferenceAwareAdvice) other;
			return (reference.equals(oth.reference));
		}
		else
			return false;
	}
}
