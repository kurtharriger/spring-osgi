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

import java.util.Arrays;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ObjectUtils;

public class ComponentElementTest extends TestCase {

	private static final String CONFIG = "basic-config.xml";

	private GenericApplicationContext context;
	private static String SIMPLE = "simple";
	private static String DEPENDS_ON = "depends-on";
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
		System.out.println("The beans declared are: " + ObjectUtils.nullSafeToString(context.getBeanDefinitionNames()));
		assertTrue("not enough beans found", context.getBeanDefinitionCount() > 6);
	}

	public void testSimple() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition(SIMPLE);
		assertEquals(Object.class.getName(), def.getBeanClassName());
	}

	public void testDependsOn() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition(DEPENDS_ON);
		assertEquals(Object.class.getName(), def.getBeanClassName());
		assertTrue(Arrays.equals(def.getDependsOn(), new String[] { SIMPLE }));
	}

	public void testDestroyMethod() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition("destroy-method");
		assertEquals(Properties.class.getName(), def.getBeanClassName());
		assertEquals("clear", def.getDestroyMethodName());
	}

	public void testLazyInit() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition("lazy-init");
		assertEquals(Object.class.getName(), def.getBeanClassName());
		assertTrue(def.isLazyInit());
	}

	public void testFactoryMethod() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition("factory-method");
		assertEquals(System.class.getName(), def.getBeanClassName());
		assertEquals("currentTimeMillis", def.getFactoryMethodName());
	}

	public void testFactoryComponent() throws Exception {
		AbstractBeanDefinition def = (AbstractBeanDefinition) context.getBeanDefinition("factory-component");
		assertNull(def.getBeanClassName());
		assertEquals("getName", def.getFactoryMethodName());
		assertEquals("thread", def.getFactoryBeanName());
	}
}
