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

package org.springframework.osgi.iandt.cm.config;

import java.util.Arrays;
import java.util.Properties;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.osgi.iandt.cm.BaseConfigurationAdminTest;

/**
 * @author Costin Leau
 */
public class CmPropertiesAndExporterTest extends BaseConfigurationAdminTest {

	private static final String DYNAMIC_OVERRIDE = "dynamic-override";
	private Properties props;


	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/osgi/iandt/cm/config/exporter-properties.xml" };
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
		Configuration cfg = configAdmin.getConfiguration(DYNAMIC_OVERRIDE);
		cfg.update(props);
	}

	public void testServiceExporter() throws Exception {
		ServiceRegistration reg = applicationContext.getBean("exporter", ServiceRegistration.class);
		ServiceReference ref = reg.getReference();
		assertEquals("Maria", ref.getProperty("Tania"));
		assertEquals("framework", ref.getProperty("spring"));
		assertNull(ref.getProperty("joe"));

		Properties newProps = new Properties();
		newProps.setProperty("spring", "osgi");
		newProps.setProperty("joe", "satriani");

		waitForCfgChangeToPropagate(DYNAMIC_OVERRIDE, newProps);

		assertEquals("framework", ref.getProperty("spring"));
		assertEquals("satriani", ref.getProperty("joe"));
		assertNull(ref.getProperty("Tania"));
	}
}