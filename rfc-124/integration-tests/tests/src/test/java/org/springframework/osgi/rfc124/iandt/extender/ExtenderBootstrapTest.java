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

package org.springframework.osgi.rfc124.iandt.extender;

import org.osgi.framework.Bundle;
import org.springframework.osgi.rfc124.iandt.BaseRFC124IntegrationTest;

/**
 * Simple test for that boostraps the RFC124 extender.
 * 
 * @author Costin Leau
 * 
 */
public class ExtenderBootstrapTest extends BaseRFC124IntegrationTest {

	protected String[] getTestBundlesNames() {
		return new String[] { "org.springframework.osgi.rfc124.iandt,simple.bundle," + getSpringDMVersion() };
	}

	public void testSanity() throws Exception {
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			System.out.println(bundle.getSymbolicName());
		}
		System.out.println("Platform started");
	}
}
