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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.osgi.service.support.ServiceWrapper;
import org.springframework.util.Assert;

/**
 * Interceptor offering static behavior around an OSGi service. If the OSGi
 * becomes unavailable, no look up or retries will be executed, the interceptor
 * throwing an exception.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceStaticInterceptor implements MethodInterceptor {

	private ServiceWrapper wrapper;

	public OsgiServiceStaticInterceptor(ServiceWrapper wrapper) {
		Assert.notNull(wrapper, "a not null service wrapper is required");
		this.wrapper = wrapper;
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

		// service has died, clean up
		if (!wrapper.isServiceAlive()) {
			wrapper.cleanup();
			throw new RuntimeException("service n/a");
		}

		return doInvoke(wrapper.getService(), invocation);
	}

}
