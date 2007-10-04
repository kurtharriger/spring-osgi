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
package org.springframework.osgi.iandt.serviceProxyFactoryBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.ServiceUnavailableException;
import org.springframework.osgi.internal.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.ServiceReferenceAware;
import org.springframework.osgi.service.importer.OsgiMultiServiceProxyFactoryBean;

public class MultiServiceProxyFactoryBeanTest extends ServiceBaseTest {

	private OsgiMultiServiceProxyFactoryBean fb;

	protected void onSetUp() throws Exception {
		fb = new OsgiMultiServiceProxyFactoryBean();
		fb.setBundleContext(bundleContext);
		ClassLoader classLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundleContext.getBundle());
		fb.setBeanClassLoader(classLoader);
	}

	protected void onTearDown() throws Exception {
		fb = null;
	}

	// causes CGLIB problems
	public void tstFactoryBeanForMultipleServicesAsInterfaces() throws Exception {

		fb.setMandatory(false);
		// look for collections
		fb.setInterface(new Class[] { ArrayList.class });
		fb.afterPropertiesSet();

		List registrations = new ArrayList(3);

		// Eek. cglib dances the bizarre initialization hula of death here. Must
		// use interfaces for now.
		try {
			Object result = fb.getObject();
			assertTrue(result instanceof Collection);
			Collection col = (Collection) result;

			assertTrue(col.isEmpty());
			Iterator iter = col.iterator();

			assertFalse(iter.hasNext());

			ArrayList a = new ArrayList();
			a.add(new Long(10));

			registrations.add(publishService(a, ArrayList.class.getName()));

			Object service = iter.next();

			assertTrue(service instanceof ArrayList);
			assertEquals(10, ((Number) ((Collection) service).toArray()[0]).intValue());

			assertFalse(iter.hasNext());
			a = new ArrayList();
			a.add(new Long(100));
			registrations.add(publishService(a, ArrayList.class.getName()));
			service = iter.next();
			assertTrue(service instanceof ArrayList);
			assertEquals(100, ((Number) ((Collection) service).toArray()[0]).intValue());
		}
		finally {
			cleanRegistrations(registrations);
		}
	}

	public void testFactoryBeanForMultipleServicesAsClasses() throws Exception {

		fb.setMandatory(false);
		fb.setInterface(new Class[] { Date.class });
		fb.afterPropertiesSet();

		List registrations = new ArrayList(3);

		long time = 321;
		Date date = new Date(time);

		try {
			Object result = fb.getObject();
			assertTrue(result instanceof Collection);
			Collection col = (Collection) result;

			assertTrue(col.isEmpty());
			Iterator iter = col.iterator();

			assertFalse(iter.hasNext());
			registrations.add(publishService(date));
			try {
				iter.next();
				fail("should have thrown exception");
			}
			catch (NoSuchElementException ex) {
				// expected
			}
			assertTrue(iter.hasNext());
			Object service = iter.next();
			assertTrue(service instanceof Date);
			assertEquals(time, ((Date) service).getTime());

			assertFalse(iter.hasNext());
			time = 111;
			date = new Date(time);
			registrations.add(publishService(date));
			assertTrue(iter.hasNext());
			service = iter.next();
			assertTrue(service instanceof Date);
			assertEquals(time, ((Date) service).getTime());
		}
		finally {
			cleanRegistrations(registrations);
		}
	}

	public void testIteratorWhenServiceGoesDown() throws Exception {
		fb.setMandatory(false);
		fb.setInterface(new Class[] { Date.class });
		fb.afterPropertiesSet();

		long time = 123;
		Date date = new Date(time);
		Properties props = new Properties();
		props.put("Moroccan", "Sunset");

		List registrations = new ArrayList(3);
		try {
			Collection col = (Collection) fb.getObject();
			Iterator iter = col.iterator();

			assertFalse(iter.hasNext());
			registrations.add(publishService(date, props));
			assertTrue(iter.hasNext());

			// deregister service
			((ServiceRegistration) registrations.remove(0)).unregister();

			// has to successed
			Object obj = iter.next();

			assertTrue(obj instanceof ServiceReferenceAware);
			assertTrue(obj instanceof Date);
			Map map = ((ServiceReferenceAware) obj).getServiceProperties();
			System.out.println(map);
			// the properties will contain the ObjectClass also
			assertTrue(map.keySet().containsAll(props.keySet()));

			try {
				// make sure the service is dead
				((Date) obj).getTime();
				fail("should have thrown exception");
			}
			catch (ServiceUnavailableException ex) {
				// proxy is dead
			}
		}
		finally {
			cleanRegistrations(registrations);
		}
	}

	private void cleanRegistrations(Collection list) {
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			((ServiceRegistration) iter.next()).unregister();
		}
	}
}
