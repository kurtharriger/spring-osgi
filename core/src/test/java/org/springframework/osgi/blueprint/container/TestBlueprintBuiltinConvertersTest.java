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
package org.springframework.osgi.blueprint.container;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Set;

import junit.framework.TestCase;

import org.osgi.service.blueprint.container.BlueprintContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.blueprint.CollectionTestComponent;
import org.springframework.osgi.blueprint.MyCustomList;
import org.springframework.osgi.blueprint.container.support.BlueprintEditorRegistrar;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;
import org.springframework.osgi.context.support.PublicBlueprintDocumentLoader;
import org.springframework.osgi.mock.MockBundleContext;

/**
 * @author Costin Leau
 */
public class TestBlueprintBuiltinConvertersTest extends TestCase {

	private static final String CONFIG = "builtin-converters.xml";

	private GenericApplicationContext context;
	private XmlBeanDefinitionReader reader;
	protected MockBundleContext bundleContext;
	private BlueprintContainer blueprintContainer;

	protected void setUp() throws Exception {
		bundleContext = new MockBundleContext();

		context = new GenericApplicationContext();
		context.setClassLoader(getClass().getClassLoader());
		context.getBeanFactory().setConversionService(new SpringBlueprintConverterService());
		context.getBeanFactory().addBeanPostProcessor(new BundleContextAwareProcessor(bundleContext));
		context.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {

			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
				beanFactory.addPropertyEditorRegistrar(new BlueprintEditorRegistrar());
			}
		});

		reader = new XmlBeanDefinitionReader(context);
		reader.setDocumentLoader(new PublicBlueprintDocumentLoader());
		reader.loadBeanDefinitions(new ClassPathResource(CONFIG, getClass()));
		context.refresh();

		blueprintContainer = new SpringBlueprintContainer(context, bundleContext);
	}

	protected void tearDown() throws Exception {
		context.close();
		context = null;
	}

	public void testConvertersAvailable() throws Exception {
		System.out.println(blueprintContainer.getComponentIds());
	}

	public void testCollection() throws Exception {
		CollectionTestComponent cpn = context.getBean("arrayToCollection", CollectionTestComponent.class);
		Object value = cpn.getPropertyValue();
		assertTrue(value instanceof Collection);
		System.out.println(value.getClass());
		Collection col = (Collection) value;
		assertEquals(3, col.size());
	}

	public void testSetToCollection() throws Exception {
		CollectionTestComponent cpn = context.getBean("setToCollection", CollectionTestComponent.class);
		Object value = cpn.getPropertyValue();
		assertTrue(value instanceof Set);
		System.out.println(value.getClass());
		Collection col = (Collection) value;
		assertEquals(2, col.size());
	}

	public void tstCustomCollection() throws Exception {
		CollectionTestComponent cpn = context.getBean("customCollection", CollectionTestComponent.class);
		Object value = cpn.getPropertyValue();
		assertTrue(value instanceof MyCustomList);
		System.out.println(value.getClass());
		Collection col = (Collection) value;
		assertEquals(2, col.size());
		cpn = context.getBean("customDictionary", CollectionTestComponent.class);
		assertTrue(cpn.getPropertyValue() instanceof Dictionary);
		System.out.println(cpn.getPropertyValue());
	}
}