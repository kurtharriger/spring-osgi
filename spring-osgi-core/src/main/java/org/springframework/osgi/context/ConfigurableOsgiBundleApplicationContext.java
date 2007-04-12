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
import org.springframework.context.ConfigurableApplicationContext;

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
	 * Flag which indicates if the application context has been fully
	 * initialized and is ready for usage or not. This is useful for
	 * implementations that rely on several 3rd party services before creating
	 * the current beans.
	 * 
	 */
	boolean isAvailable(); 

	/**
	 * Publish the application context as an OSGi service.
	 * 
	 * @param publishContextAsService The publishContextAsService to set.
	 */
	void setPublishContextAsService(boolean publishContextAsService);


    /**
     * Add a reference factory which refers to an OSGi service dependency
     * for this context
     * 
     * @param reference
     */
    void addReference(Object reference);
}
