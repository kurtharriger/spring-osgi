/*
 * Copyright 2002-20067 the original author or authors.
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
 *
 */
package org.springframework.osgi.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.osgi.internal.util.ConfigUtils;
import org.springframework.osgi.internal.util.TrackingUtil;
import org.xml.sax.EntityResolver;

/**
 * XML specific application context backed by an OSGi bundle.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 * @author Andy Piper
 * @author Hal Hildebrand
 */

// TODO: provide means to access OSGi services etc. through this application
// context?
// TODO: think about whether restricting config files to bundle: is the right
// thing to do
public class OsgiBundleXmlApplicationContext extends AbstractDelegatedExecutionApplicationContext {

	public OsgiBundleXmlApplicationContext(String[] configLocations) {
		setDisplayName("Unbound OsgiBundleXmlApplicationContext");
		setConfigLocations(configLocations);
	}

	protected String[] getDefaultConfigLocations() {
		return new String[] { ConfigUtils.SPRING_CONTEXT_FILES };
	}

	/**
	 * Loads the bean definitions via an XmlBeanDefinitionReader.
	 * 
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 * @see #initBeanDefinitionReader
	 * @see #loadBeanDefinitions
	 */
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
		// Create a new XmlBeanDefinitionReader for the given BeanFactory.
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// Configure the bean definition reader with this context's
		// resource loading environment.
		beanDefinitionReader.setResourceLoader(this);

		NamespaceHandlerResolver nsResolver = lookupNamespaceHandlerResolver();
		EntityResolver enResolver = lookupEntityResolver();

		beanDefinitionReader.setEntityResolver(enResolver);
		beanDefinitionReader.setNamespaceHandlerResolver(nsResolver);

		// Allow a subclass to provide custom initialization of the reader,
		// then proceed with actually loading the bean definitions.
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	private NamespaceHandlerResolver lookupNamespaceHandlerResolver() {
		return (NamespaceHandlerResolver) TrackingUtil.getService(new Class[] { NamespaceHandlerResolver.class }, null,
			getClass().getClassLoader(), getBundleContext(), new DefaultNamespaceHandlerResolver(getClassLoader()));
	}

	private EntityResolver lookupEntityResolver() {
		return (EntityResolver) TrackingUtil.getService(new Class[] { EntityResolver.class }, null,
			getClass().getClassLoader(), getBundleContext(), new ResourceEntityResolver(this));
	}

	/**
	 * Allows subclasses to do custom initialization here.
	 * 
	 * @param beanDefinitionReader
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
	}

	/**
	 * Load the bean definitions with the given XmlBeanDefinitionReader.
	 * <p>
	 * The lifecycle of the bean factory is handled by the refreshBeanFactory
	 * method; therefore this method is just supposed to load and/or register
	 * bean definitions.
	 * <p>
	 * Delegates to a ResourcePatternResolver for resolving location patterns
	 * into Resource instances.
	 * 
	 * @throws org.springframework.beans.BeansException in case of bean
	 * registration errors
	 * @throws java.io.IOException if the required XML document isn't found
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (int i = 0; i < configLocations.length; i++) {
				reader.loadBeanDefinitions(configLocations[i]);
			}
		}
	}
}
