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
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.service.ServiceUnavailableException;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean.ReferenceClassLoadingOptions;

/**
 * @author Costin Leau
 * 
 */
public class OsgiServiceDynamicInterceptorTest extends TestCase {

	private OsgiServiceDynamicInterceptor interceptor;

	private ServiceReference reference, ref2, ref3;

	private Object service, serv2, serv3;

	private String serv2Filter;

	private ServiceListener listener;

	protected void setUp() throws Exception {
		service = new Object();
		serv2 = new Object();
		serv3 = new Object();

		reference = new MockServiceReference();
		ref2 = new MockServiceReference();
		ref3 = new MockServiceReference();

		serv2Filter = "serv2";

		BundleContext ctx = new MockBundleContext() {

			public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
				if (serv2Filter.equals(filter))
					return new ServiceReference[] { ref2 };
				
				return new ServiceReference[] { reference };
			}

			public Object getService(ServiceReference ref) {
				if (reference == ref) {
					return service;
				}
				if (ref2 == ref) {
					return serv2;
				}

				if (ref3 == ref) {
					return serv3;
				}

				// simulate a non available service
				return null;
			}

			public void addServiceListener(ServiceListener list, String filter) throws InvalidSyntaxException {
				listener = list;
			}
		};

		interceptor = new OsgiServiceDynamicInterceptor(ctx, ReferenceClassLoadingOptions.UNMANAGED);
		interceptor.getRetryTemplate().setRetryNumbers(3);
		interceptor.getRetryTemplate().setWaitTime(1);
		interceptor.afterPropertiesSet();
	}

	protected void tearDown() throws Exception {
		service = null;
		interceptor = null;
		listener = null;
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

	/**
	 * Test method for
	 * {@link org.springframework.osgi.service.support.cardinality.OsgiServiceDynamicInterceptor#getTarget()}.
	 */
	public void testGetTarget() throws Throwable {
		// add service
		ServiceEvent event = new ServiceEvent(ServiceEvent.REGISTERED, reference);
		listener.serviceChanged(event);

		Object target = interceptor.getTarget();
		assertSame("target not properly discovered", service, target);
	}

	public void testGetTargetWhenMultipleServicesAreAvailable() throws Throwable {
		// add service
		ServiceEvent event = new ServiceEvent(ServiceEvent.REGISTERED, reference);
		listener.serviceChanged(event);

		event = new ServiceEvent(ServiceEvent.REGISTERED, ref2);
		listener.serviceChanged(event);

		Object target = interceptor.getTarget();
		assertSame("target not properly discovered", service, target);

		interceptor.setFilter(serv2Filter);
		event = new ServiceEvent(ServiceEvent.UNREGISTERING, reference);
		listener.serviceChanged(event);

		try {
			target = interceptor.getTarget();
		}
		catch (ServiceUnavailableException sue) {
			fail("target not rebound after service is down");
		}

		assertSame("wrong service rebound", serv2, target);
	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.service.support.cardinality.OsgiServiceDynamicInterceptor#afterPropertiesSet()}.
	 */
	public void testAfterPropertiesSet() {
		assertNotNull("should have initialized listener", listener);
	}

}
