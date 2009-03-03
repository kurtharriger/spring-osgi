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

package org.springframework.osgi.blueprint.metadata;

import java.util.Set;

/**
 * Basic test for the ModuleContext API. Some of the metadata calls are checked
 * by different tests.
 * 
 * 
 * @author Costin Leau
 */
public class ModuleContextApiTest extends BaseMetadataTest {

	@Override
	protected String getConfig() {
		return "/org/springframework/osgi/blueprint/config/mixed-rfc124-beans.xml";
	}

	public void testComponentNames() throws Exception {
		Set<String> names = (Set<String>) moduleContext.getComponentNames();
		assertEquals(6, names.size());
	}

	public void testBundleContext() {
		assertSame(bundleContext, moduleContext.getBundleContext());
	}

	public void testComponent() {
		checkBeanAssertion("simple-component");
		checkBeanAssertion("nested-bean");
	}

	private void checkBeanAssertion(String name) {
		assertSame(applicationContext.getBean(name), moduleContext.getComponent(name));
	}

	public void testComponentMetadata() {
		assertNotNull(moduleContext.getComponentMetadata("simple-component"));
		assertNotNull(moduleContext.getComponentMetadata("nested-bean"));
	}
}