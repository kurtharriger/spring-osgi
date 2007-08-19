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
package org.springframework.osgi.iandt.platform;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.osgi.test.platform.MBSProPlatform;
import org.springframework.osgi.test.platform.OsgiPlatform;

/**
 * @author Costin Leau
 * 
 */
public class MBSProPlatformTest extends OsgiPlatformTest {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.platform.AbstractOsgiPlatformTestCase#assertCorrectPlatform()
	 */
	protected void assertCorrectPlatform(BundleContext context) {
		String vendor = context.getProperty(Constants.FRAMEWORK_VENDOR);
		assertEquals("ProSyst", vendor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.platform.AbstractOsgiPlatformTestCase#createOsgiPlatform()
	 */
	protected OsgiPlatform createOsgiPlatform() {
		return new MBSProPlatform();
	}

}
