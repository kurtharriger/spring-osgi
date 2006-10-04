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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.ContextLoaderListener;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * WebApplicationContext implementation that understands Spring/OSGi extensions.
 * This class is not very complicated, the key issue is getting hold of a
 * reference to the Bundle/BundleContext which the servlet container is running
 * inside. This is not so simple since we cannot rely on the ContextClassLoader
 * having been set appropriately. Instead, we assume that the container has
 * initialized a singleton with the appropriate information.
 * 
 * @author Andy Piper
 * 
 */
// TODO: use AbstractOsgiRefreshableApplicationContext
public class OsgiWebApplicationContext extends XmlWebApplicationContext implements ConfigurableWebApplicationContext {
	private Bundle osgiBundle;
	private BundleContext osgiBundleContext;

	public OsgiWebApplicationContext() {
		this.osgiBundleContext = LocalBundleContext.getContext();
		if (osgiBundleContext == null) {
			throw new IllegalStateException("No BundleContext available for [" + WebApplicationContext.class.getName()
					+ "]");
		}
		this.osgiBundle = this.osgiBundleContext.getBundle();
	}

	/**
	 * Get the OSGi BundleContext for this application context
	 */
	public BundleContext getBundleContext() {
		return this.osgiBundleContext;
	}

	/**
	 * We can't look in META-INF across bundles when using osgi, so we need to
	 * change the default namespace handler (spring.handlers) location with a
	 * custom resolver. Each Spring OSGi bundle which provides a namespace
	 * handler plugin publishes a NamespaceHandlerResolver service as well as an
	 * EntityResolver service which is used to plug in the namespaces supported
	 * by the bundle.
	 * 
	 * This implementation delegates to the usual suspects if no appropriate
	 * OSGi service is found.
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
		// TODO - HSH: Needs Review. Don't like using a static to pass the
		// plugins, but may be the only practical mechanism
		beanDefinitionReader.setEntityResolver(ContextLoaderListener.plugins());
		beanDefinitionReader.setNamespaceHandlerResolver(ContextLoaderListener.plugins());
	}

	/**
	 * Implementation of getResource that delegates to the bundle for a
	 * reference starting with "bundle:"
	 */
	public Resource getResource(String location) {
		Assert.notNull(location, "location is required");
		if (location.startsWith(AbstractBundleXmlApplicationContext.BUNDLE_URL_PREFIX)) {
			return OsgiResourceUtils.getResourceFromBundle(
					location.substring(AbstractBundleXmlApplicationContext.BUNDLE_URL_PREFIX.length()), osgiBundle);
		}
		else if (location.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
			return OsgiResourceUtils.getResourceFromBundleClasspath(
					location.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length()), osgiBundle);
		}
		else {
			return super.getResource(location);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		super.postProcessBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(new BundleContextAwareProcessor(this.osgiBundleContext));
		beanFactory.ignoreDependencyInterface(BundleContextAware.class);
	}
}
