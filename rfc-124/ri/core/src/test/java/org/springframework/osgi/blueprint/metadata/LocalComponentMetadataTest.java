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
import org.osgi.service.blueprint.reflect.ListValue;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.osgi.service.blueprint.reflect.PropertiesValue;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.TypedStringValue;
import org.osgi.service.blueprint.reflect.Value;

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
		LocalComponentMetadata localMetadata = (LocalComponentMetadata) metadata;
		assertEquals("the registered name doesn't match the component name", name, localMetadata.getName());
		return localMetadata;
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

	public void testConstructorAndNestedValueTypes() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("nestedArgs");
		List<ParameterSpecification> params = localMetadata.getConstructorInjectionMetadata().getParameterSpecifications();
		assertEquals(1, params.size());
		ParameterSpecification param = params.get(0);
		assertEquals(String.class.getName(), param.getTypeName());
		System.out.println("Param value is " + param.getValue());
		TypedStringValue stv = (TypedStringValue) param.getValue();
		assertEquals(Boolean.class.getName(), stv.getTypeName());
	}

	public void testConstructorAndNestedValueWOTypes() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("nestedArgs2");
		List<ParameterSpecification> params = localMetadata.getConstructorInjectionMetadata().getParameterSpecifications();
		assertEquals(1, params.size());
		ParameterSpecification param = params.get(0);
		assertEquals(boolean.class.getName(), param.getTypeName());
		System.out.println("Param value is " + param.getValue());
		TypedStringValue stv = (TypedStringValue) param.getValue();
		assertEquals(null, stv.getTypeName());
	}

	public void testNanDouble() throws Exception {
		System.out.println(moduleContext.getComponent("nan"));
		LocalComponentMetadata localMetadata = getLocalMetadata("set");
		System.out.println(localMetadata.getPropertyInjectionMetadata());
	}

	public void testCollectionWithDefaultType() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("listWDefaultType");
		PropertyInjectionMetadata prop = (PropertyInjectionMetadata) localMetadata.getPropertyInjectionMetadata().iterator().next();
		ListValue listValue = (ListValue) prop.getValue();
		assertEquals(Double.class.getName(), listValue.getValueType());
		List<TypedStringValue> list = listValue.getList();
		for (TypedStringValue typedString : list) {
			assertNull(typedString.getTypeName());
		}
	}

	public void testPropertiesMetadata() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("props");
		PropertyInjectionMetadata prop = (PropertyInjectionMetadata) localMetadata.getPropertyInjectionMetadata().iterator().next();
		PropertiesValue propsValue = (PropertiesValue) prop.getValue();
		assertEquals("two", propsValue.getPropertiesValue().getProperty("one"));
	}

	public void testNestedRef() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("nestedRef");

	}

	// SPR-5554
	public void testStaticFactoryArgumentsOrder() throws Exception {
		System.err.println("********* test DISABLED");
		if (false) {
			LocalComponentMetadata localMetadata = getLocalMetadata("staticFactory");
			List<ParameterSpecification> params = localMetadata.getConstructorInjectionMetadata().getParameterSpecifications();

			for (ParameterSpecification param : params) {
				System.out.println("StaticFactory param " + param.getIndex());
			}

			assertEquals(3, params.size());
			assertEquals(1, params.get(0).getIndex());
			assertEquals(2, params.get(1).getIndex());
			assertEquals(0, params.get(2).getIndex());
		}
	}

	public void testFactoryMethod() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("noArgStaticFactory");
		MethodInjectionMetadata methodMetadata = localMetadata.getFactoryMethodMetadata();
		assertNotNull(methodMetadata);
		assertEquals("staticMethod", methodMetadata.getName());
		List params = methodMetadata.getParameterSpecifications();
		assertNotNull(params);
		assertTrue(params.isEmpty());
	}

	public void testFactoryArgMethod() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("oneArgStaticFactory");
		assertNull(localMetadata.getFactoryComponent());
		MethodInjectionMetadata methodMetadata = localMetadata.getFactoryMethodMetadata();
		assertNotNull(methodMetadata);
		assertEquals("staticMethod", methodMetadata.getName());
		List params = methodMetadata.getParameterSpecifications();
		assertNotNull(params);
		assertEquals(1, params.size());
	}

	public void testInstanceFactoryMethod() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("noArgInstanceFactory");
		MethodInjectionMetadata methodMetadata = localMetadata.getFactoryMethodMetadata();
		assertNotNull(methodMetadata);
		assertEquals("instanceMethod", methodMetadata.getName());
		List params = methodMetadata.getParameterSpecifications();
		assertNotNull(params);
		assertTrue(params.isEmpty());
		Value factoryComponent = localMetadata.getFactoryComponent();
		assertTrue(factoryComponent instanceof ReferenceValue);
		assertEquals("instanceFactory", ((ReferenceValue) factoryComponent).getComponentName());
	}

	public void testInstanceFactoryArgMethod() throws Exception {
		LocalComponentMetadata localMetadata = getLocalMetadata("oneArgInstanceFactory");
		MethodInjectionMetadata methodMetadata = localMetadata.getFactoryMethodMetadata();
		assertNotNull(methodMetadata);
		assertEquals("instanceMethod", methodMetadata.getName());
		List params = methodMetadata.getParameterSpecifications();
		assertNotNull(params);
		assertEquals(1, params.size());
		Value factoryComponent = localMetadata.getFactoryComponent();
		assertTrue(factoryComponent instanceof ReferenceValue);
		assertEquals("instanceFactory", ((ReferenceValue) factoryComponent).getComponentName());
	}
}