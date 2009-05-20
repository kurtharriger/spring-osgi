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

import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.springframework.beans.factory.config.TypedStringValue;

/**
 * @author Costin Leau
 */
public class LocalComponentMetadataTest extends BaseMetadataTest {

	@Override
	protected String getConfig() {
		return "/org/springframework/osgi/blueprint/config/component-subelements.xml";
	}

	private BeanMetadata getLocalMetadata(String name) {
		ComponentMetadata metadata = BlueprintContainer.getComponentMetadata(name);
		assertTrue(metadata instanceof BeanMetadata);
		BeanMetadata localMetadata = (BeanMetadata) metadata;
		assertEquals("the registered name doesn't match the component name", name, localMetadata.getId());
		return localMetadata;
	}

	public void testConstructorArg() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("constructor-arg");
		assertEquals(Integer.class.getName(), localMetadata.getClassName());
		List<BeanArgument> list = localMetadata.getArguments();
		assertNotNull(list);
		assertEquals(1, list.size());
		BeanArgument param = list.get(0);
		assertEquals(0, param.getIndex());
		assertEquals("int", param.getValueType());
		assertEquals("3", ((ValueMetadata) param.getValue()).getStringValue());
	}

	public void testValueInlined() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("propertyValueInline");
		assertEquals(Socket.class.getName(), localMetadata.getClassName());
		List<BeanProperty> props = localMetadata.getProperties();
		assertEquals(1, props.size());
		BeanProperty prop = props.iterator().next();
		assertEquals("keepAlive", prop.getName());
		assertTrue(prop.getValue() instanceof ValueMetadata);
	}

	public void testNullProperty() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("null");
		Collection<BeanProperty> props = localMetadata.getProperties();
		assertEquals(1, props.size());
		BeanProperty prop = props.iterator().next();
		assertEquals("propA", prop.getName());
		assertEquals(NullMetadata.NULL, prop.getValue());
	}

	public void testConstructorAndNestedValueTypes() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("nestedArgs");
		List<BeanArgument> params = localMetadata.getArguments();
		assertEquals(1, params.size());
		BeanArgument param = params.get(0);
		assertEquals(String.class.getName(), param.getValueType());
		System.out.println("Param value is " + param.getValue());
		ValueMetadata stv = (ValueMetadata) param.getValue();
		assertEquals(Boolean.class.getName(), stv.getTypeName());
	}

	public void testConstructorAndNestedValueWOTypes() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("nestedArgs2");
		List<BeanArgument> params = localMetadata.getArguments();
		assertEquals(1, params.size());
		BeanArgument param = params.get(0);
		assertEquals(boolean.class.getName(), param.getValueType());
		System.out.println("Param value is " + param.getValue());
		ValueMetadata stv = (ValueMetadata) param.getValue();
		assertEquals(null, stv.getTypeName());
	}

	public void testNanDouble() throws Exception {
		System.out.println(BlueprintContainer.getComponent("nan"));
		BeanMetadata localMetadata = getLocalMetadata("set");
		System.out.println(localMetadata.getProperties());
	}

	public void testCollectionWithDefaultType() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("listWDefaultType");
		BeanProperty prop = (BeanProperty) localMetadata.getProperties().iterator().next();
		CollectionMetadata listValue = (CollectionMetadata) prop.getValue();
		assertEquals(List.class, listValue.getCollectionClass());
		assertEquals(Double.class.getName(), listValue.getValueTypeName());
		List<ValueMetadata> list = listValue.getValues();
		for (ValueMetadata valueString : list) {
			assertNull(valueString.getTypeName());
		}
	}

	public void testPropertiesMetadata() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("props");
		BeanProperty prop = (BeanProperty) localMetadata.getProperties().iterator().next();
		PropsMetadata propsValue = (PropsMetadata) prop.getValue();
		System.out.println(propsValue);
		// assertEquals("two", propsValue.getEntries().getProperty("one"));
	}

	public void testNestedRef() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("nestedRef");

	}

	// SPR-5554
	public void testStaticFactoryArgumentsOrder() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("staticFactory");
		List<BeanArgument> args = localMetadata.getArguments();

		for (BeanArgument arg : args) {
			System.out.println("StaticFactory param " + arg.getIndex());
		}

		assertEquals(3, args.size());
		assertEquals(1, args.get(0).getIndex());
		assertEquals(2, args.get(1).getIndex());
		assertEquals(0, args.get(2).getIndex());
	}

	public void testFactoryMethod() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("noArgStaticFactory");
		assertEquals("staticMethod", localMetadata.getFactoryMethodName());
		List<BeanArgument> params = localMetadata.getArguments();
		assertNotNull(params);
		assertTrue(params.isEmpty());
		assertTrue(localMetadata.getArguments().isEmpty());
	}

	public void testFactoryArgMethod() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("oneArgStaticFactory");
		assertNull(localMetadata.getFactoryComponent());
		assertEquals("staticMethod", localMetadata.getFactoryMethodName());
		List<BeanArgument> params = localMetadata.getArguments();
		assertNotNull(params);
		assertEquals(1, params.size());
		assertTrue(localMetadata.getProperties().isEmpty());
	}

	public void testInstanceFactoryMethod() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("noArgInstanceFactory");
		assertEquals("instanceMethod", localMetadata.getFactoryMethodName());
		assertTrue(localMetadata.getArguments().isEmpty());
		Target factoryComponent = localMetadata.getFactoryComponent();
		assertTrue(factoryComponent instanceof RefMetadata);
		assertEquals("instanceFactory", ((RefMetadata) factoryComponent).getComponentId());
		assertTrue(localMetadata.getProperties().isEmpty());
	}

	public void testInstanceFactoryArgMethod() throws Exception {
		BeanMetadata localMetadata = getLocalMetadata("oneArgInstanceFactory");
		assertEquals("instanceMethod", localMetadata.getFactoryMethodName());
		List params = localMetadata.getArguments();
		assertNotNull(params);
		assertEquals(1, params.size());
		Target factoryComponent = localMetadata.getFactoryComponent();
		assertTrue(factoryComponent instanceof RefMetadata);
		assertEquals("instanceFactory", ((RefMetadata) factoryComponent).getComponentId());
		assertTrue(localMetadata.getProperties().isEmpty());
	}
}