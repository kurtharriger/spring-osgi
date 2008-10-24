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

package org.springframework.osgi.blueprint.config;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.blueprint.TestComponent;
import org.springframework.util.ObjectUtils;

/**
 * 
 * @author Costin Leau
 * 
 */
public class ComponentSubElementTest extends TestCase {

	private static final String CONFIG = "component-subelements.xml";

	private GenericApplicationContext context;
	private XmlBeanDefinitionReader reader;


	protected void setUp() throws Exception {
		context = new GenericApplicationContext();
		context.setClassLoader(getClass().getClassLoader());
		reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(new ClassPathResource(CONFIG, getClass()));
		context.refresh();
	}

	protected void tearDown() throws Exception {
		context.close();
		context = null;
	}

	public void testNumberOfBeans() throws Exception {
		assertTrue("not enough beans found", context.getBeanDefinitionCount() > 4);
	}

	public void testConstructorArg() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition("constructor-arg");
		assertEquals(Integer.class.getName(), def.getBeanClassName());
		assertEquals("description", def.getDescription());
		ValueHolder argumentValue = def.getConstructorArgumentValues().getArgumentValue(0, int.class);
		assertNotNull(argumentValue);
	}

	public void testConstructorRef() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition("constructor-arg-ref");
		assertEquals(String.class.getName(), def.getBeanClassName());
		assertEquals("description2", def.getDescription());
		assertEquals(1, def.getConstructorArgumentValues().getArgumentCount());
	}

	public void testPropertyInline() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition("propertyValueInline");
		assertEquals(Socket.class.getName(), def.getBeanClassName());
		MutablePropertyValues propertyValues = def.getPropertyValues();
		PropertyValue propertyValue = propertyValues.getPropertyValue("keepAlive");
		assertNotNull(propertyValue);
		assertTrue(propertyValue.getValue() instanceof BeanMetadataElement);
	}

	public void testValueRef() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition("propertyValueRef");
		assertEquals(Socket.class.getName(), def.getBeanClassName());
		assertNotNull(def.getPropertyValues().getPropertyValue("sendBufferSize"));
	}

	public void testpropertyValueNested() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition("propertyValueNested");
		assertEquals(Socket.class.getName(), def.getBeanClassName());
		PropertyValue nested = def.getPropertyValues().getPropertyValue("sendBufferSize");
		assertTrue(nested.getValue() instanceof BeanDefinitionHolder);
	}

	public void testList() throws Exception {
		TestComponent cmpn = (TestComponent) context.getBean("list");
		Object prop = cmpn.getPropA();
		assertTrue(prop instanceof List);
		List<?> list = (List<?>) prop;
		assertEquals("value", list.get(0));
		assertEquals("idref", list.get(1));
		assertNull(list.get(2));
		assertSame(context.getBean("idref"), list.get(3));
	}

	public void testSet() throws Exception {
		TestComponent cmpn = (TestComponent) context.getBean("set");
		Object prop = cmpn.getPropA();
		assertTrue(prop instanceof Set);
		Set<?> set = (Set<?>) prop;
		assertTrue(set.contains("value"));
		assertTrue(set.contains("idref"));
		assertTrue(set.contains(null));
		assertTrue(set.contains(context.getBean("idref")));
	}

	public void testMap() throws Exception {
		TestComponent cmpn = (TestComponent) context.getBean("map");
		Object prop = cmpn.getPropA();
		assertTrue(prop instanceof Map);
		Map<?, ?> map = (Map) prop;
		assertEquals("bar", map.get("foo"));
		assertEquals(context.getBean("set"), map.get(context.getBean("list")));
		assertEquals(context.getBean("list"), map.get(context.getBean("set")));
	}

	public void testProps() throws Exception {
		TestComponent cmpn = (TestComponent) context.getBean("props");
		Object prop = cmpn.getPropA();
		assertTrue(prop instanceof Properties);
		Properties props = (Properties) prop;
		assertEquals("two", props.get("one"));
		assertEquals("smith", props.get("aero"));
	}
}