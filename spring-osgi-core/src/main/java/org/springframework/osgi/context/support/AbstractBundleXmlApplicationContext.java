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
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.osgi.service.OsgiServiceUtils;

/**
 * Application context backed by an OSGi bundle. Will use the bundle classpath
 * for resource loading for any unqualified resource string. <p/> Also
 * understands the "bundle:" resource prefix for explicit loading of resources
 * from the bundle. When the bundle prefix is used the target resource must be
 * contained within the bundle (or attached fragments), the classpath is not
 * searched. <p/>
 * 
 * TODO: provide means to access OSGi services etc. through this application
 * context?
 * 
 * TODO: think about whether restricting config files to bundle: is the right
 * thing to do
 * 
 * TODO: listen to parent application context service, automatically rebind and
 * refresh if it goes away and comes back
 * 
 * @author Adrian Colyer
 * @author Andy Piper
 * @author Hal Hildebrand
 * @author Costin Leau
 * @since 2.0
 */
public class AbstractBundleXmlApplicationContext extends AbstractRefreshableOsgiBundleApplicationContext {
	public static final String BUNDLE_URL_PREFIX = "bundle:";
	public static final String APPLICATION_CONTEXT_SERVICE_NAME_HEADER = "org-springframework-context-service-name";
	public static final String NAMESPACE_HANDLER_RESOLVERS_HEADER = "Spring-Namespaces";
	public static final String PARENT_CONTEXT_SERVICE_NAME_HEADER = "SpringParentContext";

	private static final String APPLICATION_CONTEXT_SERVICE_POSTFIX = "-springApplicationContext";

	/** Used for publishing the app context * */
	private ServiceRegistration serviceRegistration;

	private NamespacePlugins namespacePlugins;

	public AbstractBundleXmlApplicationContext(BundleContext context, String[] configLocations) {
		this(null, context, configLocations);
	}

	public AbstractBundleXmlApplicationContext(BundleContext context, String[] configLocations,
			ClassLoader classLoader, NamespacePlugins namespacePlugins) {
		this(null, context, configLocations, classLoader, namespacePlugins);
	}

	public AbstractBundleXmlApplicationContext(ApplicationContext parent, BundleContext context,
			String[] configLocations) {
		this(parent, context, configLocations, null, null);
	}

	public AbstractBundleXmlApplicationContext(ApplicationContext parent, BundleContext context,
			String[] configLocations, ClassLoader classLoader, NamespacePlugins namespacePlugins) {
		super(parent);

		if (parent == null)
			setParent(getParentApplicationContext(context));

		setBundleContext(context);
		setConfigLocations(configLocations);
		// TODO: Costin (the classloader is already wrapped by ResourceLoader)
		if (classLoader != null)
			this.setClassLoader(classLoader);
		this.setDisplayName("AbstractBundleXmlApplicationContext(bundle=" + getBundleName() + ", config="
				+ getConfigName() + ")");

		this.namespacePlugins = namespacePlugins;
	}

	private String getConfigName() {
		return (getConfigLocations() == null || getConfigLocations().length < 1) ? "<no config>"
				: getConfigLocations()[0];
	}

	protected String getBundleName() {
		Bundle osgiBundle = getBundle();
		return (String) (osgiBundle.getSymbolicName() == null ? osgiBundle.getHeaders().get(Constants.BUNDLE_NAME)
				: osgiBundle.getSymbolicName());
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
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// Allow a subclass to provide custom initialization of the reader,
		// then proceed with actually loading the bean definitions.
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
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
		beanDefinitionReader.setEntityResolver(namespacePlugins);
		beanDefinitionReader.setNamespaceHandlerResolver(namespacePlugins);
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
	 *             registration errors
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
	 * The name we will use when publishing this application context as an OSGi
	 * service. If the APPLICATION_CONTEXT_SERVICE_NAME_HEADER manifest header
	 * is present, we use the user given name, otherwise we derive a name from
	 * the bundle symbolic name.
	 */
	protected String getServiceName() {
		String userSpecifiedName = (String) getBundle().getHeaders().get(APPLICATION_CONTEXT_SERVICE_NAME_HEADER);
		if (userSpecifiedName != null) {
			return userSpecifiedName;
		}
		else {
			return getBundleName() + APPLICATION_CONTEXT_SERVICE_POSTFIX;
		}
	}

	protected void publishContextAsOsgiService() {
		Dictionary serviceProperties = new Properties();
		serviceProperties.put(APPLICATION_CONTEXT_SERVICE_NAME_HEADER, getServiceName());
		this.serviceRegistration = getBundleContext().registerService(
				new String[] { ApplicationContext.class.getName() }, this, serviceProperties);
	}

	public void close() {
		try {
			if (this.serviceRegistration != null) {
				this.serviceRegistration.unregister();
			}
		}
		catch (IllegalStateException alreadyUnregisteredException) {
			// TODO: I don't like silently swallowing this
			// since we don't expect anyone else to have unregistered the
			// service,
			// we should probably log this (nothing more severe since we're
			// shutting down
			// anyway).
		}
		super.close();
	}

	public boolean isAvailable() {
		return true;
	}

	protected String createParentContextFilter(String parentContextServiceName) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(");
		buffer.append(APPLICATION_CONTEXT_SERVICE_NAME_HEADER);
		buffer.append("=");
		buffer.append(parentContextServiceName);
		buffer.append(")");
		return buffer.toString();
	}

	protected ApplicationContext getParentApplicationContext(BundleContext context) {
		String parentContextServiceName = (String) context.getBundle().getHeaders().get(
				PARENT_CONTEXT_SERVICE_NAME_HEADER);

		if (parentContextServiceName == null) {
			return null;
		}
		else {
			// try to find the service
			String filter = createParentContextFilter(parentContextServiceName);
			ServiceReference ref = OsgiServiceUtils.getService(context, ApplicationContext.class, filter);
			ApplicationContext parent = (ApplicationContext) context.getService(ref);
			// TODO: register as service listener..., probably in a proxy to the
			// app context
			// that we create here and return instead.

			return parent;
		}
	}
}
