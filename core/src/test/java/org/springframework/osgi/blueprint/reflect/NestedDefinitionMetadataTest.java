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
package org.springframework.osgi.blueprint.reflect;

import java.util.Collection;

import junit.framework.TestCase;

import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.blueprint.container.SpringBlueprintContainer;
import org.springframework.osgi.blueprint.container.support.BlueprintEditorRegistrar;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;
import org.springframework.osgi.context.support.PublicBlueprintDocumentLoader;
import org.springframework.osgi.mock.MockBundleContext;

/**
 * @author Costin Leau
 */
public class NestedDefinitionMetadataTest extends TestCase {

	private static final String CONFIG = "nested-managers.xml";

	private GenericApplicationContext context;
	private XmlBeanDefinitionReader reader;
	protected MockBundleContext bundleContext;
	private BlueprintContainer blueprintContainer;

	protected void setUp() throws Exception {
		bundleContext = new MockBundleContext();

		context = new GenericApplicationContext();
		context.setClassLoader(getClass().getClassLoader());
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

	public void testFirstLevel() throws Exception {
		String name = "first-level";
		BeanDefinition def = context.getBeanDefinition(name);
		Collection<ComponentMetadata> metadata = ComponentMetadataFactory.buildNestedMetadata(name, def);
		assertEquals(2, metadata.size());
	}

	public void testDeeplyNested() throws Exception {
		String name = "deeply-nested";
		BeanDefinition def = context.getBeanDefinition(name);
		Collection<ComponentMetadata> metadata = ComponentMetadataFactory.buildNestedMetadata(name, def);
		assertEquals(3, metadata.size());
	}

	public void testCollectionNested() throws Exception {
		String name = "nested-collection";
		BeanDefinition def = context.getBeanDefinition(name);
		Collection<ComponentMetadata> metadata = ComponentMetadataFactory.buildNestedMetadata(name, def);
		assertEquals(3, metadata.size());
	}

	public void testNestedBeans() throws Exception {
		String name = "nested-beans";
		BeanDefinition def = context.getBeanDefinition(name);
		Collection<ComponentMetadata> metadata = ComponentMetadataFactory.buildNestedMetadata(name, def);
		assertEquals(4, metadata.size());
	}

	public void testNestedServices() throws Exception {
		String name = "nested-references";
		BeanDefinition def = context.getBeanDefinition(name);
		Collection<ComponentMetadata> metadata = ComponentMetadataFactory.buildNestedMetadata(name, def);
		assertEquals(2, metadata.size());
	}

	public void testOverallMetadata() throws Exception {

		BeanDefinition def = new GenericBeanDefinition();
		assertEquals(new SpringComponentMetadata(null, def), new SpringComponentMetadata("foo", def));

		Collection<ComponentMetadata> metadata = blueprintContainer.getMetadata(ComponentMetadata.class);

		for (ComponentMetadata componentMetadata : metadata) {
			if (componentMetadata instanceof ServiceReferenceMetadata) {
				System.out.println(componentMetadata.getId());
			}
		}
		// 1+1+3+4+4+5+3+1=22
		assertEquals(22, metadata.size());
		System.out.println(blueprintContainer.getComponentIds());
	}
}