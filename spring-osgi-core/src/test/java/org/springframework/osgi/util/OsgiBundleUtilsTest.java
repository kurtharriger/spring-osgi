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
package org.springframework.osgi.util;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.springframework.osgi.mock.MockBundle;

/**
 * @author Costin Leau
 * 
 */
public class OsgiBundleUtilsTest extends TestCase {

	private Bundle bundle;

	private static int state;

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		OsgiBundleUtilsTest.state = Bundle.UNINSTALLED;
		bundle = new MockBundle() {
			public int getState() {
				return state;
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		bundle = null;
	}

	public void testGetBundleStateAsName() throws Exception {
		OsgiBundleUtilsTest.state = Bundle.ACTIVE;
		assertEquals("ACTIVE", OsgiBundleUtils.getBundleStateAsString(bundle));
		OsgiBundleUtilsTest.state = Bundle.STARTING;
		assertEquals("STARTING", OsgiBundleUtils.getBundleStateAsString(bundle));
		OsgiBundleUtilsTest.state = Bundle.STOPPING;
		assertEquals("STOPPING", OsgiBundleUtils.getBundleStateAsString(bundle));
		OsgiBundleUtilsTest.state = -123;
		assertEquals("UNKNOWN STATE", OsgiBundleUtils.getBundleStateAsString(bundle));
	}

	public void testIsInActiveBundleState() throws Exception {
		OsgiBundleUtilsTest.state = Bundle.ACTIVE;
		assertTrue(OsgiBundleUtils.isBundleActive(bundle));
		
		OsgiBundleUtilsTest.state = Bundle.STARTING;
		assertTrue(OsgiBundleUtils.isBundleActive(bundle));
		
		OsgiBundleUtilsTest.state = Bundle.INSTALLED;
		assertFalse(OsgiBundleUtils.isBundleActive(bundle));
	}
}
