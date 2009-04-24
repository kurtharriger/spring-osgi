/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.blueprint.extender.internal.activator;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.context.ModuleContextListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.osgi.service.importer.support.Cardinality;
import org.springframework.osgi.service.importer.support.CollectionType;
import org.springframework.osgi.service.importer.support.OsgiServiceCollectionProxyFactoryBean;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

/**
 * Management class sending notifications to ModuleContextListener services. The
 * class deals with the management of the listener services.
 * 
 * @author Costin Leau
 * 
 */
class ModuleContextListenerManager implements ModuleContextListener, DisposableBean {

	/** logger */
	private static final Log log = LogFactory.getLog(ModuleContextListenerManager.class);

	private volatile DisposableBean cleanupHook;
	private volatile List<ModuleContextListener> listeners;


	public ModuleContextListenerManager(BundleContext context) {
		OsgiServiceCollectionProxyFactoryBean fb = new OsgiServiceCollectionProxyFactoryBean();
		fb.setBundleContext(context);
		fb.setCardinality(Cardinality.C_0__N);
		fb.setCollectionType(CollectionType.LIST);
		fb.setInterfaces(new Class[] { ModuleContextListener.class });
		fb.setBeanClassLoader(BundleDelegatingClassLoader.createBundleClassLoaderFor(context.getBundle()));
		fb.afterPropertiesSet();

		cleanupHook = fb;
		listeners = (List<ModuleContextListener>) fb.getObject();
	}

	public void destroy() {
		if (cleanupHook != null) {
			try {
				cleanupHook.destroy();
			}
			catch (Exception ex) {
				// just log
				log.warn("Cannot destroy listeners collection", ex);
			}
			cleanupHook = null;
		}
	}

	public void contextCreated(Bundle bundle) {
		for (ModuleContextListener listener : listeners) {
			try {
				listener.contextCreated(bundle);
			}
			catch (Exception ex) {
				log.warn("#contextCreated threw exception when calling listener " + System.identityHashCode(listener),
					ex);
			}
		}
	}

	public void contextCreationFailed(Bundle bundle, Throwable ex) {
		for (ModuleContextListener listener : listeners) {
			try {
				listener.contextCreationFailed(bundle, ex);
			}
			catch (Exception excep) {
				log.warn("#contextCreationFailed threw exception when calling listener "
						+ System.identityHashCode(listener), excep);
			}
		}
	}
}
