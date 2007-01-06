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
package org.springframework.osgi.service.support.cardinality;

import org.aopalliance.intercept.MethodInvocation;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.service.NoSuchServiceException;
import org.springframework.osgi.service.support.DefaultRetryCallback;
import org.springframework.osgi.service.support.ServiceWrapper;
import org.springframework.util.Assert;

/**
 * Interceptor for x..1 cardinality.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiSingleCardinalityInterceptor extends OsgiCardinalityInterceptor {

	private boolean mandatoryEnd;

	private ServiceWrapper wrapper;

	public OsgiSingleCardinalityInterceptor() {
		this(true);
	}

	public OsgiSingleCardinalityInterceptor(boolean mandatoryEnd) {
		this.mandatoryEnd = mandatoryEnd;
	}

	protected ServiceWrapper lookupService() {

		return (ServiceWrapper) retryTemplate.execute(new DefaultRetryCallback() {
			public Object doWithRetry() {
				ServiceReference ref = context.getServiceReference(clazz);
				return (ref != null ? new ServiceWrapper(ref, context) : null);
			}
		});
	}

	protected Object doInvoke(Object service, MethodInvocation invocation) throws Throwable {
		Assert.notNull(service, "service should not be null!");
		return invocation.getMethod().invoke(service, invocation.getArguments());
	}

	/*
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object target = null;

		// check if we already have a reference
		if (wrapper != null && wrapper.isServiceAlive()) {
			target = wrapper.getService();
		}
		// lookup again for a new service
		if (target == null) {
			wrapper = lookupService();
			if (wrapper != null)
				target = wrapper.getService();
		}

		// nothing found
		if (target == null) {
			// handle the 1.. case
			if (mandatoryEnd) {
				throw new NoSuchServiceException("could not find service", null, null);
			}
			// it's a 0.. case just
			return null;
		}

		return doInvoke(target, invocation);
	}
}
