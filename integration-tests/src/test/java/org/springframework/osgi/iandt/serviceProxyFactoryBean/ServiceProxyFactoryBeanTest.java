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
package org.springframework.osgi.iandt.serviceProxyFactoryBean;

import java.io.Serializable;
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
import org.springframework.osgi.service.ServiceReferenceAware;
import org.springframework.osgi.util.OsgiFilterUtils;

/**
 * @author Costin Leau
 * 
 */
public class ServiceProxyFactoryBeanTest extends ServiceBaseTest {

	public void testFactoryBeanForOneServiceAsClass() throws Exception {
		long time = 1234;
		Date date = new Date(time);
		ServiceRegistration reg = publishService(date);

		fb.setCardinality("1..1");
		fb.setInterface(new Class[] { Date.class });
		fb.afterPropertiesSet();

		try {
			Object result = fb.getObject();
			assertTrue(result instanceof Date);
			assertEquals(time, ((Date) result).getTime());
		}
		finally {
			if (reg != null)
				reg.unregister();
		}
	}

	public void testFactoryBeanForOneServiceAsInterface() throws Exception {
		long time = 1234;
		Date date = new Date(time);

		Class[] intfs = new Class[] { Comparable.class, Serializable.class, Cloneable.class };

		String[] classes = new String[] { Comparable.class.getName(), Serializable.class.getName(),
				Cloneable.class.getName(), Date.class.getName() };

		ServiceRegistration reg = publishService(date, classes);

		fb.setCardinality("1..1");
		fb.setInterface(intfs);
		fb.setFilter(OsgiFilterUtils.unifyFilter(Date.class, null));
		fb.afterPropertiesSet();

		try {
			Object result = fb.getObject();
			// the interfaces are implemented
			assertTrue(result instanceof Comparable);
			assertTrue(result instanceof Serializable);
			assertTrue(result instanceof Cloneable);
			// but not the class
			assertFalse(result instanceof Date);
			// compare the strings
			assertEquals(result.toString(), date.toString());
		}
		finally {
			if (reg != null)
				reg.unregister();
		}
	}

	// causes CGLIB problems
	public void tstFactoryBeanForMultipleServicesAsInterfaces() throws Exception {

		fb.setCardinality("0..N");
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

		fb.setCardinality("0..N");
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
		fb.setCardinality("0..N");
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
