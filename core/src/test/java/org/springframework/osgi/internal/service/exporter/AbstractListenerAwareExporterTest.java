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
package org.springframework.osgi.internal.service.exporter;

import java.util.HashMap;

import junit.framework.TestCase;

import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.mock.MockServiceRegistration;
import org.springframework.osgi.service.util.OsgiServiceRegistrationListener;

public class AbstractListenerAwareExporterTest extends TestCase {

	private AbstractListenerAwareExporter exporter;

	protected void setUp() throws Exception {
		SimpleOsgiServiceRegistrationListener.REGISTERED = 0;
		SimpleOsgiServiceRegistrationListener.UNREGISTERED = 0;

		exporter = new AbstractListenerAwareExporter() {

			protected void registerService() {
			}

			protected void unregisterService() {
			}

		};
		exporter.setListeners(new OsgiServiceRegistrationListener[] { new SimpleOsgiServiceRegistrationListener() });
	}

	protected void tearDown() throws Exception {
		exporter = null;
	}

	public void testNotifyListenersOnRegistration() {
		assertEquals(0, SimpleOsgiServiceRegistrationListener.REGISTERED);
		assertEquals(0, SimpleOsgiServiceRegistrationListener.UNREGISTERED);

		exporter.notifyListeners(new HashMap(), new MockServiceRegistration());

		assertEquals(1, SimpleOsgiServiceRegistrationListener.REGISTERED);
		assertEquals(0, SimpleOsgiServiceRegistrationListener.UNREGISTERED);
	}

	public void testNotifyListenersOnUnregistration() {
		assertEquals(0, SimpleOsgiServiceRegistrationListener.REGISTERED);
		assertEquals(0, SimpleOsgiServiceRegistrationListener.UNREGISTERED);

		ServiceRegistration reg = exporter.notifyListeners(new HashMap(), new MockServiceRegistration());
		assertTrue(reg instanceof ServiceRegistrationWrapper);
		reg.unregister();

		assertEquals(1, SimpleOsgiServiceRegistrationListener.REGISTERED);
		assertEquals(1, SimpleOsgiServiceRegistrationListener.UNREGISTERED);
	}

}
