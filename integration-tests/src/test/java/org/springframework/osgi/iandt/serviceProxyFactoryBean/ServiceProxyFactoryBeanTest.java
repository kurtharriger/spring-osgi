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

import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
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

	public void testFactoryBeanForMultipleServicesAsInterfaces() throws Exception {

		fb.setCardinality("0..N");
		// look for collections
		fb.setInterface(new Class[] { Collection.class });
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

			registrations.add(publishService(a, Collection.class.getName()));

			Object service = iter.next();

			assertTrue(service instanceof Collection);
			assertEquals(10, ((Number) ((Collection) service).toArray()[0]).intValue());

			assertFalse(iter.hasNext());
			a = new ArrayList();
			a.add(new Long(100));
			registrations.add(publishService(a, Collection.class.getName()));
			service = iter.next();
			assertTrue(service instanceof Collection);
			assertEquals(100, ((Number) ((Collection) service).toArray()[0]).intValue());
		}
		finally {
			for (int i = 0; i < registrations.size(); i++) {
				((ServiceRegistration) registrations.get(i)).unregister();
			}
		}
	}

	public void tstFactoryBeanForMultipleServicesAsClasses() throws Exception {

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
			Object service = iter.next();
			assertTrue(service instanceof Date);
			assertEquals(time, ((Date) service).getTime());

			assertFalse(iter.hasNext());
			time = 111;
			date = new Date(time);
			registrations.add(publishService(date));
			service = iter.next();
			assertTrue(service instanceof Date);
			assertEquals(time, ((Date) service).getTime());
		}
		finally {
			for (int i = 0; i < registrations.size(); i++) {
				((ServiceRegistration) registrations.get(i)).unregister();
			}
		}
	}

}
