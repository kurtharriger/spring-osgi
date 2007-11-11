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
import org.springframework.context.ApplicationContext;
import org.xml.sax.EntityResolver;

/**
 * Standalone XML application context, backed by an OSGi bundle.
 * 
 * <p>
 * The config location defaults can be overridden via
 * {@link #getDefaultConfigLocations()}, Config locations can either denote
 * concrete files like "/myfiles/context.xml" or Ant-style patterns like
 * "/myfiles/*-context.xml" (see the
 * {@link org.springframework.util.AntPathMatcher} javadoc for pattern details).
 * </p>
 * 
 * <p>
 * Note: In case of multiple config locations, later bean definitions will
 * override ones defined in earlier loaded files. This can be leveraged to
 * deliberately override certain bean definitions via an extra XML file.
 * </p>
 * 
 * <p/> <b>This is the main ApplicationContext class for OSGi environments.</b>
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 * @author Andy Piper
 * @author Hal Hildebrand
 */
public class OsgiBundleXmlApplicationContext extends AbstractDelegatedExecutionApplicationContext {

	/** Default config location for the root context(s) */
	public static final String DEFAULT_CONFIG_LOCATION = "/META-INF/spring/*.xml";

	/**
	 * 
	 * Create a new OsgiBundleXmlApplicationContext with no parent.
	 * 
	 */
	public OsgiBundleXmlApplicationContext() {
		this((String[]) null);
	}

	/**
	 * Create a new OsgiBundleXmlApplicationContext with the given parent
	 * context.
	 * 
	 * @param parent the parent context
	 */
	public OsgiBundleXmlApplicationContext(ApplicationContext parent) {
		this(null, parent);
	}

	/**
	 * Create a new OsgiBundleXmlApplicationContext with the given
	 * configLocations.
	 * 
	 * @param configLocations array of configuration resources
	 */
	public OsgiBundleXmlApplicationContext(String[] configLocations) {
		this(configLocations, null);
	}

	/**
	 * Create a new OsgiBundleXmlApplicationContext with the given
	 * configLocations and parent context.
	 * 
	 * @param configLocations array of configuration resources
	 * @param parent the parent context
	 */
	public OsgiBundleXmlApplicationContext(String[] configLocations, ApplicationContext parent) {
		super(parent);
		setConfigLocations(configLocations);
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

		// Allow a subclass to provide custom initialisation of the reader,
		// then proceed with actually loading the bean definitions.
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	private NamespaceHandlerResolver lookupNamespaceHandlerResolver() {
		return (NamespaceHandlerResolver) TrackingUtil.getService(new Class[] { NamespaceHandlerResolver.class }, null,
			getClassLoader(), getBundleContext(), new DefaultNamespaceHandlerResolver(getClassLoader()));
	}

	private EntityResolver lookupEntityResolver() {
		return (EntityResolver) TrackingUtil.getService(new Class[] { EntityResolver.class }, null, getClassLoader(),
			getBundleContext(), new ResourceEntityResolver(this));
	}

	/**
	 * Allows subclasses to do custom initialisation here.
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

	/**
	 * Provide default locations for XML files. This implementation returns
	 * 
	 * <em>
	 * META-INF/spring/*.xml
	 * </em>
	 * 
	 * relying on the default resource environment for actual localisation.
	 * 
	 * <p/> By default, the bundle space will be used for locating the
	 * resources.
	 * 
	 * @return xml default config locations
	 */
	protected String[] getDefaultConfigLocations() {
		return new String[] { DEFAULT_CONFIG_LOCATION };
	}
}
