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
package org.springframework.osgi.test.integration;

import java.lang.reflect.Method;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.osgi.test.AbstractOsgiTests;
import org.springframework.osgi.test.JUnitTestActivator;
import org.springframework.osgi.test.AbstractOnTheFlyBundleCreatorTests;
import org.springframework.osgi.test.OsgiJUnitTest;

/**
 * Tests that AbstractOsgiTests and subclasses can be delegated to rather than
 * inherited from.
 * 
 * @author Jeremy Wales
 * @author Costin Leau
 */
public class DelegationToHierarchyTest extends TestCase implements OsgiJUnitTest {

	private AbstractOsgiTests osgiDelegate = new AbstractOnTheFlyBundleCreatorTests() {
		protected Manifest getManifest() {
			Manifest manifest = new Manifest();
			Attributes attributes = manifest.getMainAttributes();
			attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, "org.springframework.osgi.test.delegation");
			attributes.putValue(Constants.BUNDLE_ACTIVATOR, JUnitTestActivator.class.getName());
			attributes.putValue(Constants.IMPORT_PACKAGE, "junit.framework,"
					+ "org.osgi.framework;specification-version=\"1.3.0\","
					+ "org.springframework.core.io;version=\"2.1\"," + "org.springframework.osgi.test");
			return manifest;
		}
	};

	public void osgiSetUp() throws Exception {
		setUp();
	}

	public void osgiTearDown() throws Exception {
		tearDown();
	}

	public void osgiRunTest() throws Throwable {
		runTest();
	}

	public void injectBundleContext(BundleContext bundleContext) {
		osgiDelegate.injectBundleContext(bundleContext);
	}

	public void testBundleContextIsAvailable() throws Exception {
		assertNotNull(invokeMethod("getBundleContext", osgiDelegate));

	}

	public void testResourceLoaderIsAvailable() throws Exception {
		assertNotNull(invokeMethod("getResourceLoader", osgiDelegate));
	}

	protected Object invokeMethod(String name, Object target) throws Exception {
		Method method = AbstractOsgiTests.class.getDeclaredMethod(name, null);
		method.setAccessible(true);
		return method.invoke(target, null);
	}

	public void run(TestResult result) {
		osgiDelegate.injectOsgiJUnitTest(this);
		osgiDelegate.setName(getName());
		osgiDelegate.run(result);
	}

}
