/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.compendium.internal.cm;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.springframework.osgi.mock.MockBundleContext;

public class ConfigurationAdminManagerTest extends TestCase {

	private ConfigurationAdminManager cam;
	private Object pid;
	private MockBundleContext bundleContext;
	private Map services;


	protected void setUp() throws Exception {
		services = new LinkedHashMap();
		bundleContext = new MockBundleContext() {

			public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
				services.put(service, properties);
				return super.registerService(clazzes, service, properties);
			}
		};

		pid = new Object();
		cam = new ConfigurationAdminManager(pid, bundleContext);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testManagedServiceRegistration() throws Exception {
		assertTrue(services.isEmpty());
		assertNull(cam.getConfiguration());
		assertNotNull(services);
		assertFalse(services.isEmpty());
		assertEquals(1, services.size());
	}

	public void testManagedServiceProperties() {
		assertTrue(services.isEmpty());
		assertNull(cam.getConfiguration());

		Dictionary props = (Dictionary) services.values().iterator().next();
		assertEquals(pid, props.get(Constants.SERVICE_PID));
	}

	public void testManagedServiceInstance() {
		assertTrue(services.isEmpty());
		assertNull(cam.getConfiguration());
		Object serviceInstance = services.keySet().iterator().next();
		assertTrue(serviceInstance instanceof ManagedService);
	}

	public void testUpdateCallback() throws Exception {
		final List holder = new ArrayList(4);

		ManagedServiceBeanManager msbm = new ManagedServiceBeanManager() {

			public Object register(Object bean) {
				return null;
			}

			public void unregister(Object bean) {
			}

			public void updated(Map properties) {
				holder.add(properties);
			}
		};
		cam.setBeanManager(msbm);
		assertTrue(services.isEmpty());
		assertNull(cam.getConfiguration());

		ManagedService callback = (ManagedService) services.keySet().iterator().next();
		Dictionary props = new Properties();
		props.put("foo", "bar");
		props.put("spring", "source");

		assertTrue(holder.isEmpty());
		callback.updated(props);
		assertEquals(1, holder.size());
		assertEquals(props, holder.get(0));
	}
}
