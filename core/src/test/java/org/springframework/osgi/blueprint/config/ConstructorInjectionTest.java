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
package org.springframework.osgi.blueprint.config;

import java.net.URL;
import java.util.Date;
import java.util.Locale;

import junit.framework.TestCase;

import org.osgi.service.blueprint.container.BlueprintContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.blueprint.TestComponent;
import org.springframework.osgi.blueprint.container.SpringBlueprintContainer;
import org.springframework.osgi.blueprint.container.SpringBlueprintConverterService;
import org.springframework.osgi.blueprint.container.support.BlueprintEditorRegistrar;
import org.springframework.osgi.context.support.PublicBlueprintDocumentLoader;

/**
 * 
 * @author Costin Leau
 */
public class ConstructorInjectionTest extends TestCase {

	private static final String CONFIG = "blueprint-construct-inject.xml";

	private GenericApplicationContext context;
	private XmlBeanDefinitionReader reader;
	private BlueprintContainer container;

	protected void setUp() throws Exception {
		context = new GenericApplicationContext();
		context.setClassLoader(getClass().getClassLoader());
		reader = new XmlBeanDefinitionReader(context);
		reader.setDocumentLoader(new PublicBlueprintDocumentLoader());
		reader.loadBeanDefinitions(new ClassPathResource(CONFIG, getClass()));
		context.getBeanFactory().setConversionService(new SpringBlueprintConverterService());
		context.refresh();
		container = new SpringBlueprintContainer(context, null);
	}

	protected void tearDown() throws Exception {
		context.close();
		context = null;
	}

	private TestComponent getComponent(String name) {
		return context.getBean(name, TestComponent.class);
	}

	private <T> T getPropA(String name) {
		TestComponent tc = getComponent(name);
		return (T) tc.getPropA();
	}

	public void tstCtrAssign() throws Exception {
		Object propA = getPropA("constructorAssign");
	}

	public void testCharArray() throws Exception {
		Object propA = getPropA("compWrappedCharArray");
		assertTrue(propA instanceof Character[]);
	}

	public void testPrimitiveShortArray() throws Exception {
		Object propA = getPropA("compPrimShortArray");
		assertTrue(propA instanceof short[]);
	}

	public void testDateArray() throws Exception {
		Date[] array = getPropA("compDateArray");
		Date date = new Date("19 Feb 2009");
		assertEquals(array[0], date);

	}

	public void testURLArray() throws Exception {
		URL[] array = getPropA("compURLArray");
		assertEquals(2, array.length);
	}

	public void testClassArray() throws Exception {
		Class<?>[] propA = getPropA("compClassArray");
		assertEquals(String.class, propA[0]);
	}

	public void testLocaleArray() throws Exception {
		Locale[] propA = getPropA("compLocaleArray");
		assertEquals(Locale.US, propA[0]);
	}

	public void testPrimitiveConstructor() throws Exception {
		try {
			Object component = context.getBean("primToWrapperArg");
			fail("Expected an ambuigity exception");
		} catch (Exception ex) {
			// expected
		}
	}

	public void testPrimitiveFactoryMethod() throws Exception {
		try {
			Object component = context.getBean("primToWrapperFactory");
			fail("Expected an ambuigity exception");
		} catch (Exception ex) {
			// expected
		}
	}

	public void testNestedValue() throws Exception {
		Object component = context.getBean("nestedURLValue");
	}

	public void testNestedValueFactory() throws Exception {
		Object component = context.getBean("nestedURLValueFactory");
	}

	public void testEmptyArray() throws Exception {
		Object component = context.getBean("emptyArrayConstruct");
	}

	public void testCollectionConversion() throws Exception {
		try {
			Object component = context.getBean("collectionConflict");
			fail("Expected an ambuigity exception");
		} catch (Exception ex) {
			// expected
		}
	}
	

	public void testCompProperties() throws Exception {
		Object component = context.getBean("compProperties");
	}
}