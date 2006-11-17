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
package org.springframework.osgi.test.propertyplaceholder;

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.osgi.context.support.OsgiPropertyPlaceholder;
import org.springframework.osgi.service.OsgiServiceUtils;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;

/**
 * @author Costin Leau
 * 
 */
public class PropertyPlaceholderTest extends ConfigurableBundleCreatorTests {

	private OsgiPropertyPlaceholder placeholder;

	private final static String ID = "PropertyPlaceholderTest-123";

	private final static Dictionary DICT = new Hashtable();

	static {
		// this is already added by the framework but it's useful when doing the assertion
		DICT.put("service.pid", ID);
		DICT.put("foo", "bar");
		DICT.put("white", "horse");
	}

	protected String getManifestLocation() {
		return "classpath:org/springframework/osgi/test/propertyplaceholder/PropertyPlaceholder.MF";
	}

	protected String[] getBundleLocations() {
		return new String[] { localMavenArtifact("org.springframework.osgi", "aopalliance.osgi", "1.0-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-aop", "2.1-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-context", "2.1-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-beans", "2.1-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-osgi-core", "1.0-SNAPSHOT"),
				// required by cm_all for logging
				localMavenArtifact("org.knopflerfish.bundles", "commons-logging_all", "2.0.0"),
				localMavenArtifact("org.knopflerfish.bundles", "cm_all", "2.0.0") };
	}

	public void onSetUp() throws Exception {
		prepareConfiguration();

		placeholder = new OsgiPropertyPlaceholder();
		placeholder.setBundleContext(getBundleContext());
		// add persistence id in here
		placeholder.setPersistentId(ID);
		placeholder.afterPropertiesSet();
	}

	// add a default table into OSGi
	private void prepareConfiguration() throws Exception {

		ServiceReference ref = OsgiServiceUtils.getService(getBundleContext(), ConfigurationAdmin.class, null);

		ConfigurationAdmin admin = (ConfigurationAdmin) getBundleContext().getService(ref);
		Configuration config = admin.getConfiguration(ID);
		config.update(DICT);
	}

	private Properties getProperties() throws Exception {
		Field field = placeholder.getClass().getDeclaredField("cmProperties");
		field.setAccessible(true);
		return (Properties) field.get(placeholder);
	}

	public void testDummy() throws Exception {
		Properties props = getProperties();
		
		assertEquals(DICT, props);
	}
}
