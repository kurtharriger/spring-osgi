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
 * Default implementation of OsgiBundleXmlApplicationContextFactory
 * 
 * @author Adrian Colyer
 * @author Andy Piper
 * @see OsgiBundleXmlApplicationContextFactory
 * @since 2.0
 */
public class DefaultOsgiBundleXmlApplicationContextFactory implements OsgiBundleXmlApplicationContextFactory {
	public AbstractBundleXmlApplicationContext createApplicationContext(BundleContext aBundleContext,
			String[] configLocations, NamespacePlugins plugins) {
		return new OsgiBundleXmlApplicationContext(aBundleContext, configLocations, plugins);
	}

	public AbstractBundleXmlApplicationContext createApplicationContextWithBundleContext(BundleContext aBundleContext,
                                                                                         String[] configLocations,
                                                                                         NamespacePlugins plugins,
                                                                                         boolean waitForDependencies) {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		BundleContext bc = LocalBundleContext.getContext();
		try {
			ClassLoader cl = BundleDelegatingClassLoader.createBundleClassLoaderFor(aBundleContext.getBundle());
			Thread.currentThread().setContextClassLoader(cl);
			LocalBundleContext.setContext(aBundleContext);

			if (waitForDependencies) {
				return new ServiceDependentBundleXmlApplicationContext(aBundleContext, configLocations, cl,
						                                               plugins);
			}
			else {
				return new OsgiBundleXmlApplicationContext(aBundleContext, configLocations, cl, plugins);
			}

		}
		finally {
			LocalBundleContext.setContext(bc);
			Thread.currentThread().setContextClassLoader(ccl);
		}
	}

}
