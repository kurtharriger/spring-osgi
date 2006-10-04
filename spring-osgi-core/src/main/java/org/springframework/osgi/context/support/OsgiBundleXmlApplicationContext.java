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
 * Created on 23-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.context.support;

import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;

/**
 * Application context backed by an OSGi bundle. Will use the bundle classpath
 * for resource loading for any unqualified resource string. <p/> Also
 * understands the "bundle:" resource prefix for explicit loading of resources
 * from the bundle. When the bundle prefix is used the target resource must be
 * contained within the bundle (or attached fragments), the classpath is not
 * searched. <p/> This application context will publish itself as a service
 * using the service name
 * "&lt;bundle-symbolic-name&gt-springApplicationContext". To specify an
 * alternate service name, use the org.springframework.context.service.name
 * manifest header in the bundle manifest. For example: <p/> <code>
 * org.springframework.context.service.name=myApplicationContextService
 * </code>
 * <p/> TODO: provide means to access OSGi services etc. through this
 * application context?
 * 
 * TODO: think about whether restricting config files to bundle: is the right
 * thing to do
 * 
 * TODO: listen to parent application context service, automatically rebind and
 * refresh if
 * 
 * 
 * TODO: it goes away and comes back
 * 
 * @author Adrian Colyer
 * @author Andy Piper
 * @author Hal Hildebrand
 * @since 2.0
 */
public class OsgiBundleXmlApplicationContext extends AbstractBundleXmlApplicationContext {

	public OsgiBundleXmlApplicationContext(BundleContext context, String[] configLocations) {
		this(context, configLocations, null);
	}

	public OsgiBundleXmlApplicationContext(BundleContext aBundleContext, String[] configLocations,
			NamespacePlugins plugins) {
		this(aBundleContext, configLocations, new BundleDelegatingClassLoader(aBundleContext.getBundle()), plugins);
	}

	public OsgiBundleXmlApplicationContext(BundleContext context, String[] configLocations, ClassLoader classLoader,
			NamespacePlugins namespacePlugins) {
		super(context, configLocations, classLoader, namespacePlugins);
	}

	public OsgiBundleXmlApplicationContext(ApplicationContext parent, BundleContext context, String[] configLocations,
			ClassLoader classLoader, NamespacePlugins plugins) {
		super(parent, context, configLocations, classLoader, plugins);

		publishContextAsOsgiService();
        
        refresh();
    }

}
