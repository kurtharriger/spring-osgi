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

import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.ServiceReferenceAware;
import org.springframework.osgi.service.importer.OsgiMultiServiceProxyFactoryBean;

/**
 * @author Costin Leau
 * 
 */
public class ServiceRefAwareWithMultiServiceTest extends ServiceBaseTest {

	private OsgiMultiServiceProxyFactoryBean fb;

	protected void onSetUp() throws Exception {
		fb = new OsgiMultiServiceProxyFactoryBean();
		fb.setBundleContext(getBundleContext());
		ClassLoader classLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(getBundleContext().getBundle());
		fb.setBeanClassLoader(classLoader);
	}

	protected void onTearDown() throws Exception {
		fb = null;
	}

	// this fails due to some CGLIB problems
	public void testProxyForMultipleCardinality() throws Exception {
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
			assertTrue(iter.hasNext());
			Object service = iter.next();
			assertTrue(service instanceof Date);
			assertEquals(time, ((Date) service).getTime());

			assertTrue(service instanceof ServiceReferenceAware);
			assertNotNull(((ServiceReferenceAware) service).getServiceReference());

			assertFalse(iter.hasNext());
			time = 111;
			date = new Date(time);
			registrations.add(publishService(date));
			assertTrue(iter.hasNext());
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
}
