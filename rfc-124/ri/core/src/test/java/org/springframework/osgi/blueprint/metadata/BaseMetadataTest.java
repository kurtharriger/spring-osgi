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

import junit.framework.TestCase;

import org.osgi.service.blueprint.container.BlueprintContainer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.blueprint.context.SpringBlueprintContainer;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;
import org.springframework.osgi.mock.MockBundleContext;

/**
 * Base class for metadata tests.
 * 
 * @author Costin Leau
 */
public abstract class BaseMetadataTest extends TestCase {

	protected GenericApplicationContext applicationContext;
	protected BlueprintContainer BlueprintContainer;
	private XmlBeanDefinitionReader reader;
	protected MockBundleContext bundleContext;

	protected void setUp() throws Exception {
		bundleContext = new MockBundleContext();

		applicationContext = new GenericApplicationContext();
		applicationContext.setClassLoader(getClass().getClassLoader());
		applicationContext.getBeanFactory().addBeanPostProcessor(new BundleContextAwareProcessor(bundleContext));
		reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(getConfig(), getClass()));
		applicationContext.refresh();
		BlueprintContainer = new SpringBlueprintContainer(applicationContext, bundleContext);
	}

	protected abstract String getConfig();

	protected void tearDown() throws Exception {
		applicationContext.close();
		applicationContext = null;
		BlueprintContainer = null;
		bundleContext = null;
	}
}