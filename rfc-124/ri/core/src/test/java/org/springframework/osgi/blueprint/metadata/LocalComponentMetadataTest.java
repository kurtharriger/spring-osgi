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

import java.net.Socket;
import java.util.Collection;
import java.util.List;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ConstructorInjectionMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.TypedStringValue;

/**
 * @author Costin Leau
 */
public class LocalComponentMetadataTest extends BaseMetadataTest {

	@Override
	protected String getConfig() {
		return "/org/springframework/osgi/blueprint/config/component-subelements.xml";
	}

	private LocalComponentMetadata getLocalMetadata(String name) {
		ComponentMetadata metadata = moduleContext.getComponentMetadata(name);
		assertTrue(metadata instanceof LocalComponentMetadata);
		return (LocalComponentMetadata) metadata;
	}

	public void testConstructorArg() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("constructor-arg");
		assertEquals(Integer.class.getName(), localMetadata.getClassName());
		ConstructorInjectionMetadata injectionMetadata = localMetadata.getConstructorInjectionMetadata();
		assertNotNull(injectionMetadata);
		List<ParameterSpecification> list = injectionMetadata.getParameterSpecifications();
		assertNotNull(list);
		assertEquals(1, list.size());
		ParameterSpecification param = list.get(0);
		assertEquals(0, param.getIndex());
		assertEquals("int", param.getTypeName());
		assertEquals("3", ((TypedStringValue) param.getValue()).getStringValue());
	}

	public void testValueInlined() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("propertyValueInline");
		assertEquals(Socket.class.getName(), localMetadata.getClassName());
		Collection<PropertyInjectionMetadata> props = localMetadata.getPropertyInjectionMetadata();
		assertEquals(1, props.size());
		PropertyInjectionMetadata prop = props.iterator().next();
		assertEquals("keepAlive", prop.getName());
		assertTrue(prop.getValue() instanceof TypedStringValue);
	}

	public void testNullProperty() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("null");
		Collection<PropertyInjectionMetadata> props = localMetadata.getPropertyInjectionMetadata();
		assertEquals(1, props.size());
		PropertyInjectionMetadata prop = props.iterator().next();
		assertEquals("propA", prop.getName());
		assertEquals(NullValue.NULL, prop.getValue());
	}
	
	public void testNanDouble() throws Exception {
		System.out.println(moduleContext.getComponent("nan"));
		
	}
}