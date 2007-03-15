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

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;

/**
 * 
 * Integration test for OsgiPropertyPlaceholder.
 * 
 * @author Costin Leau
 * 
 */
public class PropertyPlaceholderTest extends ConfigurableBundleCreatorTests {

	private final static String ID = "PropertyPlaceholderTest-123";

	private final static Dictionary DICT = new Hashtable();

	private ConfigurableApplicationContext ctx;

	static {
		DICT.put("foo", "bar");
		DICT.put("white", "horse");
		// This is needed when running under KF
		System.setProperty("com.gatespace.bundle.cm.store", System.getProperty("user.dir"));
	}

	protected String getManifestLocation() {
		return "classpath:org/springframework/osgi/test/propertyplaceholder/PropertyPlaceholder.MF";
	}

	protected String[] getBundles() {
		return new String[] { localMavenArtifact("org.springframework.osgi", "aopalliance.osgi", "1.0-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-aop", "2.1-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-beans", "2.1-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-context", "2.1-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-osgi-core", "1.0-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-osgi-extender", "1.0-SNAPSHOT"),
				// required by cm_all for logging
				localMavenArtifact("org.knopflerfish.bundles", "log_all", "2.0.0"),
				localMavenArtifact("org.knopflerfish.bundles", "cm_all", "2.0.0") };
	}

	protected void onSetUp() throws Exception {
		prepareConfiguration();

		ctx = new OsgiBundleXmlApplicationContext(getBundleContext(),
				new String[] { "org/springframework/osgi/test/propertyplaceholder/placeholder.xml" });
		ctx.refresh();
	}

	protected void onTearDown() throws Exception {
		if (ctx != null)
			ctx.close();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.test.ConfigurableBundleCreatorTests#getBundleContentPattern()
	 */
	protected String[] getBundleContentPattern() {
		return super.getBundleContentPattern();
	}

	// add a default table into OSGi
	private void prepareConfiguration() throws Exception {

		ServiceReference ref = OsgiServiceUtils.getService(getBundleContext(), ConfigurationAdmin.class, null);

		ConfigurationAdmin admin = (ConfigurationAdmin) getBundleContext().getService(ref);
		Configuration config = admin.getConfiguration(ID);
		config.update(DICT);
	}

	public void testFoundProperties() throws Exception {
		String bean = (String) ctx.getBean("bean1");
		assertEquals("horse", bean);
	}

	public void testFallbackProperties() throws Exception {
		String bean = (String) ctx.getBean("bean2");
		assertEquals("treasures", bean);
	}
}
