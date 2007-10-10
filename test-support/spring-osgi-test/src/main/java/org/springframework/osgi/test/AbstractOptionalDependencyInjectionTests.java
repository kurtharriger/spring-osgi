/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.test;

import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.context.support.AbstractDelegatedExecutionApplicationContext;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.util.ObjectUtils;

/**
 * JUnit superclass, which creates an empty OSGi bundle appCtx when no
 * configuration file is specified. Required for mixing Spring existing testing
 * hierarchy with the OSGi testing framework functionality.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractOptionalDependencyInjectionTests extends AbstractDependencyInjectionSpringContextTests {

	/**
	 * Empty OSGi application context that doesn't require any files to be
	 * specified.
	 * 
	 * Useful to still get injection of bundleContext and OSGi specific resource
	 * loading.
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class EmptyOsgiApplicationContext extends AbstractDelegatedExecutionApplicationContext {

		protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException, BeansException {
		}

	}

	public AbstractOptionalDependencyInjectionTests() {
		super();
	}

	public AbstractOptionalDependencyInjectionTests(String name) {
		super(name);
	}

	protected ConfigurableApplicationContext createApplicationContext(String[] locations) {
		ConfigurableOsgiBundleApplicationContext context = null;

		if (ObjectUtils.isEmpty(locations))
			context = new EmptyOsgiApplicationContext();
		else
			context = new OsgiBundleXmlApplicationContext(locations);

		context.setBundleContext(getBundleContext());
		context.refresh();
		return context;
	}

	/**
	 * Return the test bundle context.
	 * 
	 * @return test bundle context
	 */
	protected abstract BundleContext getBundleContext();

}
