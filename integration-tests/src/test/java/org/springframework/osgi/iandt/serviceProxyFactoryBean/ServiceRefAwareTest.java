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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceRegistration;
import org.springframework.aop.SpringProxy;
import org.springframework.osgi.service.ServiceReferenceAware;
import org.springframework.osgi.util.MapBasedDictionary;

/**
 * Integration test for ServiceFactoryAware interface.
 * 
 * @author Costin Leau
 * 
 */
public class ServiceRefAwareTest extends ServiceBaseTest {

	public void testProxyForUnaryCardinality() throws Exception {
		long time = 1234;
		Date date = new Date(time);
		Dictionary dict = new MapBasedDictionary();
		ServiceRegistration reg = publishService(date);

		fb.setCardinality("1..1");
		fb.setInterface(new Class[] { Date.class });
		fb.afterPropertiesSet();

		ServiceReferenceAware refAware = null;
		try {
			Object result = fb.getObject();
			assertTrue(result instanceof Date);
			// check it's our object
			assertEquals(time, ((Date) result).getTime());
			assertTrue(result instanceof SpringProxy);
			assertTrue(result instanceof ServiceReferenceAware);

			refAware = (ServiceReferenceAware) result;
			assertNotNull(refAware.getServiceReference());
		}
		finally {
			if (reg != null)
				reg.unregister();
		}

		// test reference after the service went down
		assertNotNull(refAware.getServiceReference());
		assertNull(refAware.getServiceReference().getBundle());
	}

	public void testServiceReferenceProperties() throws Exception {

		long time = 1234;
		Date date = new Date(time);
		Dictionary dict = new MapBasedDictionary();
		dict.put("foo", "bar");
		dict.put("george", "michael");

		ServiceRegistration reg = publishService(date, dict);

		fb.setCardinality("1..1");
		fb.setInterface(new Class[] { Date.class });
		fb.afterPropertiesSet();

		try {
			Object result = fb.getObject();
			assertTrue(result instanceof Date);
			// check it's our object
			assertEquals(time, ((Date) result).getTime());

			ServiceReferenceAware refAware = (ServiceReferenceAware) result;
			// get properties
			Map map = refAware.getServiceProperties();

			// compare content
			assertTrue(doesMapContainsDictionary(dict, map));

			// update properties dynamically
			dict.put("another", "property");

			reg.setProperties(dict);

			assertTrue(doesMapContainsDictionary(dict, map));
		}
		finally {
			if (reg != null)
				reg.unregister();
		}
	}

	// this fails due to some CGLIB problems
	public void tstProxyForMultipleCardinality() throws Exception {
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

			assertTrue(service instanceof ServiceReferenceAware);
			assertNotNull(((ServiceReferenceAware) service).getServiceReference());

			assertFalse(iter.hasNext());
			time = 111;
			date = new Date(time);
			registrations.add(publishService(date));
			service = iter.next();
			assertTrue(service instanceof Date);
			assertEquals(time, ((Date) service).getTime());
			assertTrue(service instanceof ServiceReferenceAware);
			assertNotNull(((ServiceReferenceAware) service).getServiceReference());

		}
		finally {
			for (int i = 0; i < registrations.size(); i++) {
				((ServiceRegistration) registrations.get(i)).unregister();
			}
		}
	}

	/**
	 * Check if the 'test' map contains the original Dictionary.
	 * 
	 * @param original
	 * @param test
	 * @return
	 */
	private boolean doesMapContainsDictionary(Dictionary original, Map test) {
		Enumeration enm = original.keys();
		while (enm.hasMoreElements()) {
			if (!test.containsKey(enm.nextElement()))
				return false;
		}

		return true;
	}

}
