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

import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.aopalliance.intercept.MethodInvocation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockServiceReference;

/**
 * @author Costin Leau
 * 
 */
public class OsgiServiceDynamicInterceptorTest extends TestCase {

	private OsgiServiceDynamicInterceptor interceptor;

	private ServiceReference reference;

	private Object service;

	protected void setUp() throws Exception {
		service = new Object();
		reference = new MockServiceReference();

		BundleContext ctx = new MockBundleContext() {

			public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
				return new ServiceReference[] { reference };
			}

			public Object getService(ServiceReference ref) {
				if (reference == ref) {
					return service;
				}
				// simulate a non available service
				return null;
			}

		};

		interceptor = new OsgiServiceDynamicInterceptor(ctx, 2);
		interceptor.getRetryTemplate().setRetryNumbers(3);
		interceptor.getRetryTemplate().setWaitTime(1);
		interceptor.afterPropertiesSet();
	}

	protected void tearDown() throws Exception {
		service = null;
		interceptor = null;
	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.service.support.cardinality.OsgiServiceDynamicInterceptor#OsgiServiceDynamicInterceptor()}.
	 */
	public void testOsgiServiceDynamicInterceptor() {
		assertNotNull(interceptor.getRetryTemplate());
	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.service.support.cardinality.OsgiServiceDynamicInterceptor#lookupService()}.
	 */
	public void testLookupService() throws Throwable {
		Object serv = interceptor.getTarget();
		assertSame(service, serv);
	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.service.support.cardinality.OsgiServiceDynamicInterceptor#doInvoke(java.lang.Object, org.aopalliance.intercept.MethodInvocation)}.
	 */
	public void testDoInvoke() throws Throwable {
		Object target = new Object();
		Method m = target.getClass().getDeclaredMethod("hashCode", null);

		MethodInvocation invocation = new ReflectiveMethodInvocation(new Object(), target, m, new Object[0], null, null);
		assertEquals(new Integer(service.hashCode()), interceptor.invoke(invocation));
	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.service.support.cardinality.OsgiServiceDynamicInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)}.
	 */
	public void testInvocationWhenServiceNA() throws Throwable {
		// service n/a

		Object target = new Object();
		Method m = target.getClass().getDeclaredMethod("hashCode", null);

		MethodInvocation invocation = new ReflectiveMethodInvocation(new Object(), target, m, new Object[0], null, null);
		ServiceReference oldRef = reference;
		reference = null;

		try {
			interceptor.invoke(invocation);
			fail("should have thrown exception");
		}
		catch (RuntimeException ex) {
			// expected
		}

		// service is up
		reference = oldRef;

		assertEquals(new Integer(service.hashCode()), interceptor.invoke(invocation));
	}

}
