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
package org.springframework.osgi.service.collection;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.support.AopUtils;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockFilter;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Unit test for the static proxies returned by Osgi collection.
 * 
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceCollectionProxiesTest extends TestCase {

	private OsgiServiceCollection col;

	private Map services;

	private String[] classInterfaces = new String[] { Cloneable.class.getName() };

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		services = new LinkedHashMap();

		BundleContext ctx = new MockBundleContext() {
			public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
				return new ServiceReference[0];
			}

			public Object getService(ServiceReference reference) {
				Object service = services.get(reference);
				return (service == null ? new Object() : service);
			}

		};

		col = new OsgiServiceCollection(new MockFilter(), ctx, getClass().getClassLoader());
		col.afterPropertiesSet();
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		col = null;
	}

	public void testHashCodeBetweenProxyAndTarget() {
		Date date = new Date(123);

		ServiceReference ref = new MockServiceReference(classInterfaces);
		services.put(ref, date);

		Object proxy = col.createServiceProxy(ref);

		System.out.println(ObjectUtils.nullSafeToString((ClassUtils.getAllInterfaces(proxy))));
		assertFalse("proxy and service should have different hashcodes", date.hashCode() == proxy.hashCode());

	}

	public void testHashCodeBetweenProxies() {
		Date date = new Date(123);

		ServiceReference ref = new MockServiceReference(classInterfaces);
		services.put(ref, date);

		Object proxy = col.createServiceProxy(ref);
		Object proxy2 = col.createServiceProxy(ref);
		assertEquals("proxies for the same service should have the same hashcode", proxy.hashCode(), proxy2.hashCode());
	}

	public void testEqualsBetweenProxyAndTarget() {
		Date date = new Date(123);

		ServiceReference ref = new MockServiceReference(classInterfaces);
		services.put(ref, date);

		Object proxy = col.createServiceProxy(ref);

		assertFalse("proxy and service should not be equal", date.equals(proxy));
	}

	public void testEqualsBetweenProxies() {
		Date date = new Date(123);

		ServiceReference ref = new MockServiceReference(classInterfaces);
		services.put(ref, date);

		Object proxy = col.createServiceProxy(ref);
		Object proxy2 = col.createServiceProxy(ref);
		assertEquals("proxies for the same target should be equal", proxy, proxy2);
	}

	public void testHashCodeBetweenProxyAndItself() {
		Date date = new Date(123);

		ServiceReference ref = new MockServiceReference(classInterfaces);
		services.put(ref, date);

		Object proxy = col.createServiceProxy(ref);

		assertEquals("proxy should consistent hashcode", proxy.hashCode(), proxy.hashCode());
	}

	public void testEqualsBetweenProxyAndItself() {
		Date date = new Date(123);

		ServiceReference ref = new MockServiceReference(classInterfaces);
		services.put(ref, date);

		Object proxy = col.createServiceProxy(ref);
		assertEquals("proxy should be equal to itself", proxy, proxy);

	}

}
