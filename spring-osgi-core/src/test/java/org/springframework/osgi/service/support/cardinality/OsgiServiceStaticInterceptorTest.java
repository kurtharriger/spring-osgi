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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.service.support.ServiceWrapper;

/**
 * @author Costin Leau
 * 
 */
public class OsgiServiceStaticInterceptorTest extends TestCase {

	private OsgiServiceStaticInterceptor interceptor;

	private ServiceWrapper wrapper;

	private ServiceReference reference;

	private Object service;

	protected void setUp() throws Exception {
		service = new Object();

		reference = new MockServiceReference();

		BundleContext ctx = new MockBundleContext() {
			public Object getService(ServiceReference reference) {
				return service;
			}
		};

		wrapper = new ServiceWrapper(reference, ctx);

		interceptor = new OsgiServiceStaticInterceptor(ctx, reference, 2);
	}

	protected void tearDown() throws Exception {
		service = null;
		wrapper = null;
		interceptor = null;
	}

	public void testNullWrapper() throws Exception {
		try {
			interceptor = new OsgiServiceStaticInterceptor(null, null, 0);
			fail("expected exception");
		}
		catch (RuntimeException ex) {
			// expected
		}
	}

	public void testInvocationOnService() throws Throwable {
		Object target = new Object();
		Method m = target.getClass().getDeclaredMethod("hashCode", null);

		MethodInvocation invocation = new ReflectiveMethodInvocation(new Object(), target, m, new Object[0], null, null);
		assertEquals(new Integer(service.hashCode()), interceptor.invoke(invocation));
	}

	public void testInvocationWhenServiceNA() throws Throwable {
		// service n/a
		ServiceReference reference = new MockServiceReference() {
			public Bundle getBundle() {
				return null;
			}
		};

		interceptor = new OsgiServiceStaticInterceptor(new MockBundleContext(), reference, 2);

		Object target = new Object();
		Method m = target.getClass().getDeclaredMethod("hashCode", null);

		MethodInvocation invocation = new ReflectiveMethodInvocation(new Object(), target, m, new Object[0], null, null);
		try {
			interceptor.invoke(invocation);
			fail("should have thrown exception");
		}
		catch (RuntimeException ex) {
			// expected
		}
	}
}
