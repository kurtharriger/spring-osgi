/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.iandt.serviceProxyFactoryBean;

import java.util.Dictionary;

import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;

/**
 * @author Costin Leau
 * 
 */
public abstract class ServiceBaseTest extends AbstractConfigurableBundleCreatorTests {

	protected OsgiServiceProxyFactoryBean fb;

	protected String[] getBundles() {
		return new String[] { localMavenArtifact("org.springframework.osgi", "cglib-nodep.osgi", "2.1.3-SNAPSHOT") };
	}

	protected void onSetUp() throws Exception {
		fb = new OsgiServiceProxyFactoryBean();
		fb.setBundleContext(getBundleContext());
		// execute retries fast
		fb.setRetryTimes(1);
		fb.setTimeout(1);
        ClassLoader classLoader =
                BundleDelegatingClassLoader.createBundleClassLoaderFor(getBundleContext().getBundle());
        fb.setBeanClassLoader(classLoader);
    }

	protected void onTearDown() throws Exception {
		fb = null;
	}

	protected ServiceRegistration publishService(Object obj, String name) throws Exception {
		return getBundleContext().registerService(name, obj, null);
	}

	protected ServiceRegistration publishService(Object obj, String names[]) throws Exception {
		return getBundleContext().registerService(names, obj, null);
	}

	protected ServiceRegistration publishService(Object obj, String names[], Dictionary dict) throws Exception {
		return getBundleContext().registerService(names, obj, null);
	}

	protected ServiceRegistration publishService(Object obj) throws Exception {
		return publishService(obj, obj.getClass().getName());
	}

	protected ServiceRegistration publishService(Object obj, Dictionary dict) throws Exception {
		return getBundleContext().registerService(obj.getClass().getName(), obj, dict);
	}

}
