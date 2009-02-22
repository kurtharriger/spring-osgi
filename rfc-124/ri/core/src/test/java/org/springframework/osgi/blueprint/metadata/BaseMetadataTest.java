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

import org.osgi.service.blueprint.context.ModuleContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.blueprint.context.SpringModuleContext;
import org.springframework.osgi.mock.MockBundleContext;

/**
 * Base class for metadata tests.
 * 
 * @author Costin Leau
 */
public abstract class BaseMetadataTest extends TestCase {

	protected GenericApplicationContext applicationContext;
	protected ModuleContext moduleContext;
	private XmlBeanDefinitionReader reader;
	protected MockBundleContext bundleContext;


	protected void setUp() throws Exception {
		applicationContext = new GenericApplicationContext();
		applicationContext.setClassLoader(getClass().getClassLoader());
		reader = new XmlBeanDefinitionReader(applicationContext);
		reader.loadBeanDefinitions(new ClassPathResource(getConfig(), getClass()));
		applicationContext.refresh();
		bundleContext = new MockBundleContext();
		moduleContext = new SpringModuleContext(applicationContext, bundleContext);
	}

	protected abstract String getConfig();

	protected void tearDown() throws Exception {
		applicationContext.close();
		applicationContext = null;
		moduleContext = null;
		bundleContext = null;
	}
}