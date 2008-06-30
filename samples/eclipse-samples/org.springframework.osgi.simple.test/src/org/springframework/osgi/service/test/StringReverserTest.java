/*
 * Copyright 2006 the original author or authors.
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
package org.springframework.osgi.service.test;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.osgi.sample.service.StringReverser;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.util.OsgiStringUtils;
/**
 * Starts up an OSGi environment and installs the
 * String Reverser service bundle and the bundles it depends on. The test
 * classes in this project will be turned into a virtual bundle 
 * which is also installed and the tests are then run inside the
 * OSGi runtime.
 * 
 * The tests have access to a BundleContext, which we use to 
 * test that the simpleService bean was indeed published as 
 * an OSGi service.
 * 
 * @author Oleg Zhurakousky
 */
public class StringReverserTest extends AbstractConfigurableBundleCreatorTests {
	private static Logger logger = Logger.getLogger(StringReverserTest.class);
	/**
	 * Installs String Reverser bundle from the specified location.
	 */
	public Resource[] getTestBundles() {
		return new Resource[] { new FileSystemResource("../org.sprigframework.osgi.simple/reverser.jar")};
	}
	/**
	 * Overrides the default resource location.
	 */
	public String getRootPath() {
		return "file:./bin";
	}
	/**
	 * Outputs platform Meta Data
	 * @throws Exception
	 */
	public void testOsgiPlatformStarts() throws Exception {
		System.out.println(bundleContext
				.getProperty(Constants.FRAMEWORK_VENDOR));
		System.out.println(bundleContext
				.getProperty(Constants.FRAMEWORK_VERSION));
		System.out.println(bundleContext
				.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT));
	}

	/**
	 * Displays all bundles installed in this Test environment
	 * @throws Exception
	 */
	public void testOsgiEnvironment() throws Exception {
		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			System.out.print(OsgiStringUtils.nullSafeName(bundles[i]));
			System.out.print(", \n");
		}
	}
	/**
	 * Tests functionality of our String Reverser service
	 */
	public void testSimpleServiceExported() {
		waitOnContextCreation("org.springframework.osgi.simple.reverser");
		ServiceReference ref = bundleContext
				.getServiceReference(StringReverser.class.getName());
		assertNotNull("Service Reference is null", ref);
		try {
			StringReverser simpleService = (StringReverser) bundleContext
					.getService(ref);
			assertNotNull("Cannot find the service", simpleService);
			logger.info("Reversed String: "
					+ simpleService.reverse("Hello Spring DM"));
		} finally {
			bundleContext.ungetService(ref);
		}
	}
}
