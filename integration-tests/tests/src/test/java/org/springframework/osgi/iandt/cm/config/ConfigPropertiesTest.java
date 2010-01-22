/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.iandt.cm.config;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.osgi.iandt.cm.BaseConfigurationAdminTest;
import org.springframework.osgi.service.exporter.support.ServicePropertiesListenerManager;

/**
 * @author Costin Leau
 */
public class ConfigPropertiesTest extends BaseConfigurationAdminTest {

	private Properties props;
	private static final String SIMPLE = "simple";
	private static final String OVERRIDE = "override";
	private static final String DYNAMIC_SIMPLE = "dynamic-simple";
	private static final String DYNAMIC_OVERRIDE = "dynamic-override";
	private static final String INIT_TIME = "init-time";
	private static final String EAGER_INIT = "eager-init";
	private static final String LAZY_INIT = "lazy-init";

	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/osgi/iandt/cm/config/config-properties.xml" };
	}

	private void initProperties() {
		props = new Properties();
		props.setProperty("Tania", "Maria");
		props.setProperty("spring", "source");
	}

	protected void onSetUp() throws Exception {
		super.onSetUp();
		initProperties();
	}

	protected void prepareConfiguration(ConfigurationAdmin configAdmin) throws Exception {
		initProperties();
		Configuration cfg = configAdmin.getConfiguration(SIMPLE);
		cfg.update(props);

		cfg = configAdmin.getConfiguration(OVERRIDE);
		cfg.update(props);

		cfg = configAdmin.getConfiguration(DYNAMIC_SIMPLE);
		cfg.update(props);

		cfg = configAdmin.getConfiguration(DYNAMIC_OVERRIDE);
		cfg.update(props);

		cfg = configAdmin.getConfiguration(INIT_TIME);
		cfg.delete();

		cfg = configAdmin.getConfiguration(EAGER_INIT);
		cfg.update(props);

		cfg = configAdmin.getConfiguration(LAZY_INIT);
		cfg.update(props);
	}

	public void testSimpleConfigAdminConfig() throws Exception {
		Object bean = applicationContext.getBean(SIMPLE);
		assertTrue(bean instanceof Properties);
		for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			assertEquals(entry.getValue(), props.get(entry.getKey()));
		}
	}

	public void testOverrideConfigAdminConfig() throws Exception {
		Object bean = applicationContext.getBean(OVERRIDE);
		assertTrue(bean instanceof Properties);
		assertFalse(props.equals(bean));
		assertEquals("framework", ((Properties) bean).getProperty("spring"));
	}

	public void testDynamicNoOverride() throws Exception {
		Object bean = applicationContext.getBean(DYNAMIC_SIMPLE);
		assertTrue(bean instanceof ServicePropertiesListenerManager);
		assertFalse(props.equals(bean));
		Properties prop = (Properties) bean;
		// make sure the properties update has been propagated
		if (prop.size() > 0) {
			assertEquals("source", prop.getProperty("spring"));
		}
		assertNull(prop.getProperty("steve"));
		Configuration cfg = cm.getConfiguration(DYNAMIC_SIMPLE);

		Properties newProps = new Properties();
		newProps.setProperty("spring", "osgi");
		newProps.setProperty("steve", "vai");

		waitForCfgChangeToPropagate(DYNAMIC_SIMPLE, newProps);
		waitForCfgChangeToPropagate(DYNAMIC_SIMPLE, newProps);
		
		assertEquals("osgi", prop.getProperty("spring"));
		assertEquals("vai", prop.getProperty("steve"));
	}

	public void testDynamicOverride() throws Exception {
		Object bean = applicationContext.getBean(DYNAMIC_OVERRIDE);
		assertTrue(bean instanceof ServicePropertiesListenerManager);
		assertFalse(props.equals(bean));
		Properties prop = (Properties) bean;
		assertEquals("framework", prop.getProperty("spring"));
		assertNull(prop.getProperty("steve"));
		Configuration cfg = cm.getConfiguration(DYNAMIC_OVERRIDE);

		Properties newProps = new Properties();
		newProps.setProperty("spring", "osgi");
		newProps.setProperty("steve", "vai");

		waitForCfgChangeToPropagate(DYNAMIC_OVERRIDE, newProps);
		waitForCfgChangeToPropagate(DYNAMIC_OVERRIDE, newProps);

		assertEquals("framework", prop.getProperty("spring"));
		assertEquals("vai", prop.getProperty("steve"));
	}

	public void testInitTimeout() throws Exception {
		// check configuration
		Configuration cfg = cm.getConfiguration(INIT_TIME);
		assertNull(cfg.getProperties());

		Thread th = new Thread(new Runnable() {
			public void run() {
				try {
					// sleep 4 seconds
					Thread.sleep(4 * 1000);
				} catch (Exception ex) {
				}

				try {
					cm.getConfiguration(INIT_TIME).update(props);
				} catch (Exception ex) {
					throw new RuntimeException("Cannot update config", ex);
				}
			}
		});

		long start = System.currentTimeMillis();
		th.start();
		// ask the bean
		Properties bean = applicationContext.getBean(INIT_TIME, Properties.class);
		long stop = System.currentTimeMillis();
		assertEquals("Maria", bean.get("Tania"));
		assertEquals("source", bean.get("spring"));
		// check that at least 4 seconds elapsed
		assertTrue(stop - start >= 4 * 1000);
	}

	public void testEagerInit() throws Exception {
		// update the props
		Configuration cfg = cm.getConfiguration(EAGER_INIT);
		Properties props = new Properties();
		props.put("spring", "dm");
		props.put("updated", "true");

		cfg.update(props);
		Properties bean = applicationContext.getBean(EAGER_INIT, Properties.class);
		assertEquals("source", bean.get("spring"));
		assertNull(bean.get("updated"));
	}

	public void testLazyInit() throws Exception {
		// update the props
		Configuration cfg = cm.getConfiguration(LAZY_INIT);
		Properties props = new Properties();
		props.put("spring", "dm");
		props.put("updated", "true");

		cfg.update(props);
		Properties bean = applicationContext.getBean(LAZY_INIT, Properties.class);
		assertEquals("dm", bean.get("spring"));
		assertEquals("true", bean.get("updated"));
	}
}