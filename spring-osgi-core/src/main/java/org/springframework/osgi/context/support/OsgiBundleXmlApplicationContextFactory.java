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
 * Created on 25-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.context.support;

import org.osgi.framework.BundleContext;

/**
 * Factory interface for the creation of OsgiBundleXmlApplicationContext.
 * Primarily needed to facilitate unit testing of types that need to 
 * create these contexts.
 * 
 * @author Adrian Colyer
 * @since 2.0
 */
public interface OsgiBundleXmlApplicationContextFactory {

	/**
	 * Create an OsgiBundleXmlApplicationContext
	 * 
	 * @param aBundleContext  the OSGi BundleContext for the bundle
	 * @param configLocations location paths for the context config files
     * @param plugins the Spring namespace plugins
	 * @return OsgiBundleXmlApplicationContext
	 */
	AbstractBundleXmlApplicationContext createApplicationContext(BundleContext aBundleContext,
                                                                 String[] configLocations,
                                                                 NamespacePlugins plugins);
	
	/**
	 * Create an OsgiBundleXmlApplicationContext in the context of a Bundle Context ClassLoader
	 *
	 * @param aBundleContext  the OSGi BundleContext for the bundle
	 * @param configLocations location paths for the context config files
     * @param plugins the Spring namespace plugins
	 * @param waitForDependencies
	 * @return OsgiBundleXmlApplicationContext
	 */
	AbstractBundleXmlApplicationContext createApplicationContextWithBundleContext(
            BundleContext aBundleContext,
            String[] configLocations,
            NamespacePlugins plugins,
            boolean waitForDependencies);
}
