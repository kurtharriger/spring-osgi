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
 * Interceptor adding dynamic behavior for unary service (..1 cardinality). It
 * will look for a service using the given class and filter name, retrying if
 * the service is down or unavailable. Will dynamically rebound a new service,
 * if one is available with a higher service ranking.
 * 
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceDynamicInterceptor extends AbstractOsgiServiceDynamicInterceptor {

	private ServiceWrapper wrapper;

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
		// if (wrapper != null && wrapper.isServiceAlive()) {
		// target = wrapper.getService();
		// }

		// TODO: [C] on each invocation the service is being looked up.
		// This can be potentially inefficient when the platform contains a lot
		// of services
		// A listener based mecahnism to do sorting based on service ranking
		// should be used
		// to cache the service and retry the lookup only if the service is
		// down.

		// lookup again for a new service
		if (target == null) {
			wrapper = lookupService();
			if (wrapper != null)
				target = wrapper.getService();
		}

		// nothing found
		if (target == null) {
			throw new NoSuchServiceException("could not find service", null, null);
		}

		return doInvoke(target, invocation);
	}
}
