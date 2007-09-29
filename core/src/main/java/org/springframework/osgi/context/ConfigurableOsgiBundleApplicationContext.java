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
 */
package org.springframework.osgi.context;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.context.support.OsgiBundleNamespaceHandlerAndEntityResolver;

/**
 * Interface to be implemented by configurable OSGi bundle application contexts.
 * 
 * <p>
 * Note: The setters of this interface need to be called before an invocation of
 * the refresh method inherited from ConfigurableApplicationContext. They do not
 * cause an initialization of the context on their own.
 * 
 * @author Costin Leau
 * 
 */
public interface ConfigurableOsgiBundleApplicationContext extends ConfigurableApplicationContext {

	/**
	 * Service entry used for specifying the application context name when
	 * published as an OSGi service
	 */
	static final String APPLICATION_CONTEXT_SERVICE_PROPERTY_NAME = "org.springframework.context.service.name";

	/**
	 * Bean name under which the OSGi bundle context is published as a
	 * singleton.
	 */
	static final String BUNDLE_CONTEXT_BEAN_NAME = "bundleContext";

	/**
	 * Set the config locations for this OSGi bundle application context. If not
	 * set, the implementation is supposed to use a default for the given
	 * namespace.
	 */
	void setConfigLocations(String[] configLocations);

	/**
	 * Set the BundleContext for this OSGi bundle application context.
	 * <p>
	 * Does not cause an initialization of the context: refresh needs to be
	 * called after the setting of all configuration properties.
	 * 
	 * @see #refresh()
	 */
	void setBundleContext(BundleContext bundleContext);

	/**
	 * Return the bundle context for this application context. This method is
	 * offered as a helper since as of OSGi 4.1, the bundle context can be
	 * discovered from a given bundle.
	 * 
	 * @see #getBundle()
	 * @return
	 */
	BundleContext getBundleContext();

	/**
	 * Return the OSGi bundle for this application context.
	 * 
	 * @return the Bundle for this OSGi bundle application context.
	 */
	Bundle getBundle();

	/**
	 * Publish the application context as an OSGi service.
	 * 
	 * @param publishContextAsService The publishContextAsService to set.
	 */
	void setPublishContextAsService(boolean publishContextAsService);

	/**
	 * @param namespaceResolver The namespaceResolver to set.
	 */
	void setNamespaceHandlerAndEntityResolver(OsgiBundleNamespaceHandlerAndEntityResolver namespaceResolver);
}
