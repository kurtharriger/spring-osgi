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

package org.springframework.osgi.iandt.cm.managedservicefactory;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPlugin;
import org.springframework.osgi.iandt.cm.BaseConfigurationAdminTest;
import org.springframework.osgi.util.OsgiServiceUtils;

/**
 * Integration test for managed-service-factory element.
 * 
 * @author Costin Leau
 */
public class ManagedServiceFactoryTest extends BaseConfigurationAdminTest {

	private Properties props;
	private final String FPID = ManagedServiceFactoryTest.class.getName();
	private final String FILTER = "(service.factoryPid=" + FPID + ")";


	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/osgi/iandt/cm/managedservicefactory/context.xml" };
	}

	private void initProperties() {
		props = new Properties();
		props.setProperty("class", System.class.getName());
		props.setProperty("integer", "54321");
	}

	protected void onSetUp() throws Exception {
		super.onSetUp();
		initProperties();
	}

	protected void prepareConfiguration(ConfigurationAdmin configAdmin) throws Exception {
		initProperties();
		Properties localCopy = (Properties) props.clone();
		localCopy.setProperty("simple", "simple");

		// prepare one instance
		Configuration cfg = configAdmin.createFactoryConfiguration(FPID, null);
		cfg.update(localCopy);
	}

	public void testConfigurationCreation() throws Exception {
		Configuration[] cfgs = cm.listConfigurations(FILTER);
		int size = (cfgs != null ? cfgs.length : 0);

		Dictionary newProps = new Properties();
		newProps.put("foo", "bar");
		updateAndWaitForConfig(newProps);

		cfgs = cm.listConfigurations(FILTER);
		assertNotNull(cfgs);
		assertEquals("new factory configuration not registered", cfgs.length, size + 1);
	}

	public void testCreateInstance() throws Exception {
		Dictionary newProps = new Properties();
		newProps.put("new", "instance");

		Collection stepA = (Collection) applicationContext.getBean("msf");
		int sizeA = stepA.size();

		updateAndWaitForConfig(newProps);

		Collection stepB = (Collection) applicationContext.getBean("msf");
		int sizeB = stepB.size();

		assertEquals(sizeA + 1, sizeB);
	}

	public void testDestroyInstance() throws Exception {
		Collection stepA = (Collection) applicationContext.getBean("msf");
		int sizeA = stepA.size();

		Configuration[] cfgs = cm.listConfigurations(FILTER);

		final Object lock = new Object();
		// delete configuration
		bundleContext.registerService(ConfigurationListener.class.getName(), new ConfigurationListener() {

			public void configurationEvent(ConfigurationEvent event) {
				if (event.getType() == ConfigurationEvent.CM_DELETED) {
					System.out.println("Deleting configuration " + event.getFactoryPid() + "|" + event.getPid());
					synchronized (lock) {
						lock.notify();
					}
				}

			}

		}, new Properties());

		cfgs[cfgs.length - 1].delete();

		synchronized (lock) {
			lock.wait(10 * 1000);
		}

		Collection stepB = (Collection) applicationContext.getBean("msf");
		
		//assertEquals(sizeA - 1, stepB.size());
	}

	private void updateAndWaitForConfig(final Dictionary properties) throws Exception {
		Dictionary props = new Hashtable(1);
		props.put(ConfigurationPlugin.CM_TARGET, FPID);

		final Object lock = new Object();

		Configuration cfg = cm.createFactoryConfiguration(FPID, null);

		ServiceRegistration reg = bundleContext.registerService(ConfigurationPlugin.class.getName(),
			new ConfigurationPlugin() {

				public void modifyConfiguration(ServiceReference reference, Dictionary prps) {
					System.out.println("Received props " + prps);

					Enumeration en = properties.keys();
					while (en.hasMoreElements()) {
						Object key = en.nextElement();
						if (!properties.get(key).equals(prps.get(key))) {
							System.out.println("Different dictionary received:\nExpected: " + properties
									+ "\nReceived:" + prps);
							return;
						}
					}
					System.out.println("Received proper configuration - disabling waiting..");
					synchronized (lock) {
						lock.notify();
					}

				}
			}, props);

		try {
			cfg.update(properties);

			synchronized (lock) {
				// wait up to 3 seconds
				lock.wait(10 * 1000);
			}
		}
		finally {
			OsgiServiceUtils.unregisterService(reg);
		}
	}
}
