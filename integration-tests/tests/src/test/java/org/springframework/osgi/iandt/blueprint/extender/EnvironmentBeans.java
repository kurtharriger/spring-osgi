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
package org.springframework.osgi.iandt.blueprint.extender;

import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.osgi.iandt.blueprint.BaseBlueprintIntegrationTest;

/**
 * @author Costin Leau
 */
public class EnvironmentBeans extends BaseBlueprintIntegrationTest {

	private static final String SYM_NAME = "org.springframework.osgi.rfc124.iandt.simpleservice";

	@Override
	protected String[] getTestBundlesNames() {
		return new String[] { "org.springframework.osgi.iandt.blueprint,simple.bundle," + getSpringDMVersion() };
	}

	public void testBlueprintContainer() throws Exception {
		Thread.sleep(1000 * 3);
		ServiceReference reference = bundleContext.getServiceReference(BlueprintContainer.class.getName());
		assertNotNull(reference);
		BlueprintContainer container = (BlueprintContainer) bundleContext.getService(reference);
		System.out.println(container.getComponentIds());
		ComponentMetadata metadata = container.getComponentMetadata("blueprintContainer");
	}
}
