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
package org.springframework.osgi.iandt;

import org.osgi.framework.Constants;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.test.platform.EquinoxPlatform;
import org.springframework.osgi.test.platform.FelixPlatform;
import org.springframework.osgi.test.platform.KnopflerfishPlatform;
import org.springframework.osgi.test.platform.OsgiPlatform;

/**
 * This test might log exceptions since the OSGi platform may try to register an
 * URLStreamFactory every time they start (during {@link #setName(String)}).
 * 
 * Basically, this just reads the underlying platform and checks its vendor to
 * make sure the mapping is correct.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiPlatformTest extends AbstractConfigurableBundleCreatorTests {

	private OsgiPlatform platform;

	protected void onSetUp() {
		platform = createPlatform();

	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.test.platform.EquinoxPlatform#start()}.
	 */
	public void testOsgiPlatform() throws Exception {

		String vendor = getBundleContext().getProperty(Constants.FRAMEWORK_VENDOR);

		if ("Eclipse".equals(vendor))
			assertTrue(platform instanceof EquinoxPlatform);
		if ("Apache Software Foundation".equals(vendor))
			assertTrue(platform instanceof FelixPlatform);
		if ("Knopflerfish".equals(vendor))
			assertTrue(platform instanceof KnopflerfishPlatform);
//		if ("ProSyst".equals(vendor))
//			assertTrue(platform instanceof MBSProPlatform);
	}
}
