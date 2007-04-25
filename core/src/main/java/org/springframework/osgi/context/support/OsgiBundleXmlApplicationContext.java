/*
 * Copyright 2002-2006 the original author or authors.
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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.osgi.util.ConfigUtils;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Application context backed by an OSGi bundle. Will use the bundle classpath
 * for resource loading for any unqualified resource string. <p/> Also
 * understands the "bundle:" resource prefix for explicit loading of resources
 * from the bundle. When the bundle prefix is used the target resource must be
 * contained within the bundle (or attached fragments), the classpath is not
 * searched. <p/>
 * 
 * @author Adrian Colyer
 * @author Andy Piper
 * @author Hal Hildebrand
 * @author Costin Leau
 * @since 2.0
 */

// TODO: provide means to access OSGi services etc. through this application
// context?
// TODO: think about whether restricting config files to bundle: is the right
// thing to do
public class OsgiBundleXmlApplicationContext extends AbstractRefreshableOsgiBundleApplicationContext {

	/** retrieved from the BundleContext * */
	private OsgiBundleNamespaceHandlerAndEntityResolver namespaceResolver;

	public OsgiBundleXmlApplicationContext(BundleContext context, String[] configLocations) {
		setBundleContext(context);
		setConfigLocations(configLocations);
		this.setDisplayName(ClassUtils.getShortName(getClass()) + "(bundle="
				+ OsgiBundleUtils.getNullSafeSymbolicName(getBundle()) + ", config="
				+ StringUtils.arrayToCommaDelimitedString(getConfigLocations()) + ")");
	}

	protected String[] getDefaultConfigLocations() {
		return new String[] { ConfigUtils.SPRING_CONTEXT_DIRECTORY };
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

		// prefer the namespace plugin instead of ResourceEntityResolver

		OsgiBundleNamespaceHandlerAndEntityResolver defaultHandlerResolver = (namespaceResolver != null ? namespaceResolver
				: lookupHandlerAndResolver());

		if (defaultHandlerResolver != null) {
			beanDefinitionReader.setEntityResolver(defaultHandlerResolver);
			beanDefinitionReader.setNamespaceHandlerResolver(defaultHandlerResolver);
		}
		else {
			// fallback to ResourceEntityResolver
			beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
		}

		// Allow a subclass to provide custom initialization of the reader,
		// then proceed with actually loading the bean definitions.
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * Lookup the NamespaceHandler and entity resolver Service inside the OSGi
	 * space.
	 * 
	 * @return
	 */
	protected OsgiBundleNamespaceHandlerAndEntityResolver lookupHandlerAndResolver() {
		// FIXME: add smart lookup proxy - if no service is available, an
		// exception will be thrown
		ServiceReference reference = OsgiServiceUtils.getService(getBundleContext(),
			OsgiBundleNamespaceHandlerAndEntityResolver.class, null);

		OsgiBundleNamespaceHandlerAndEntityResolver resolver = (OsgiBundleNamespaceHandlerAndEntityResolver) getBundleContext().getService(
			reference);

		if (logger.isDebugEnabled())
			logger.debug("looking for NamespaceHandlerAndEntityResolver OSGi service.... found=" + resolver);

		return resolver;
	}

	/**
	 * We can't look in META-INF across bundles when using osgi, so we need to
	 * change the default namespace handler (spring.handlers) location with a
	 * custom resolver. Each Spring OSGi bundle which provides a namespace
	 * handler plugin publishes a NamespaceHandlerResolver service as well as an
	 * EntityResolver service which is used to plug in the namespaces supported
	 * by the bundle.
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
		if (namespaceResolver != null) {
			beanDefinitionReader.setEntityResolver(namespaceResolver);
			beanDefinitionReader.setNamespaceHandlerResolver(namespaceResolver);
		}
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
	 * @param namespaceResolver The namespaceResolver to set.
	 */
	public void setNamespaceResolver(OsgiBundleNamespaceHandlerAndEntityResolver namespaceResolver) {
		this.namespaceResolver = namespaceResolver;
	}

}
