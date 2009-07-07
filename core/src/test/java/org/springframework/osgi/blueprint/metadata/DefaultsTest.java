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

import java.util.Collection;
import java.util.List;

import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.springframework.osgi.blueprint.TestComponent;

/**
 * 
 * @author Costin Leau
 * 
 */
public class DefaultsTest extends BaseMetadataTest {

	@Override
	protected String getConfig() {
		return "/org/springframework/osgi/blueprint/config/blueprint-defaults.xml";
	}

	public void testDefaultsOnNestedBeans() throws Exception {
		ComponentMetadata metadata = blueprintContainer.getComponentMetadata("nested");
		assertEquals(ComponentMetadata.ACTIVATION_LAZY, metadata.getActivation());
		BeanMetadata meta = (BeanMetadata) metadata;
		List<BeanProperty> props = meta.getProperties();
		assertEquals(2, props.size());
		BeanProperty propA = props.get(0);
		ReferenceMetadata nestedRef = (ReferenceMetadata) propA.getValue();
		assertEquals(ReferenceMetadata.AVAILABILITY_MANDATORY, nestedRef.getAvailability());
		assertEquals(300, nestedRef.getTimeout());

		BeanProperty propB = props.get(1);
		ReferenceListMetadata nestedList = (ReferenceListMetadata) propB.getValue();
		assertEquals(ReferenceMetadata.AVAILABILITY_OPTIONAL, nestedList.getAvailability());
		assertEquals(ReferenceListMetadata.USE_SERVICE_REFERENCE, nestedList.getMemberType());
	}

	public void testBeanInstances() throws Exception {
		TestComponent componentInstance = (TestComponent) blueprintContainer.getComponentInstance("nested");
		Collection propB = (Collection) componentInstance.getPropB();
		System.out.println(propB.size());
	}
}