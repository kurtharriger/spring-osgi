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

package org.springframework.osgi.extender.internal.blueprint.activator;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.osgi.service.importer.support.Availability;
import org.springframework.osgi.service.importer.support.CollectionType;
import org.springframework.osgi.service.importer.support.OsgiServiceCollectionProxyFactoryBean;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

/**
 * Management class sending notifications to ModuleContextListener services. The class deals with the management of the
 * listener services.
 * 
 * @author Costin Leau
 * 
 */
class BlueprintListenerManager implements BlueprintListener, DisposableBean {

	/** logger */
	private static final Log log = LogFactory.getLog(BlueprintListenerManager.class);

	private volatile DisposableBean cleanupHook;
	private volatile List<BlueprintListener> listeners;
	private final Timer eventDispatcher = new Timer("BlueprintEvent Dispatcher", true);

	private class Task extends TimerTask {

		private final BlueprintEvent event;

		Task(BlueprintEvent event) {
			this.event = event;
		}

		@Override
		public void run() {
			for (BlueprintListener listener : listeners) {
				try {
					listener.blueprintEvent(event);
				} catch (Exception ex) {
					log.warn("exception encountered when calling listener " + System.identityHashCode(listener), ex);
				}
			}
		}
	}

	public BlueprintListenerManager(BundleContext context) {
		OsgiServiceCollectionProxyFactoryBean fb = new OsgiServiceCollectionProxyFactoryBean();
		fb.setBundleContext(context);
		fb.setAvailability(Availability.OPTIONAL);
		fb.setCollectionType(CollectionType.LIST);
		fb.setInterfaces(new Class[] { BlueprintListener.class });
		fb.setBeanClassLoader(BundleDelegatingClassLoader.createBundleClassLoaderFor(context.getBundle()));
		fb.afterPropertiesSet();

		cleanupHook = fb;
		listeners = (List) fb.getObject();
	}

	public void destroy() {
		eventDispatcher.cancel();
		eventDispatcher.purge();

		if (cleanupHook != null) {
			try {
				cleanupHook.destroy();
			} catch (Exception ex) {
				// just log
				log.warn("Cannot destroy listeners collection", ex);
			}
			cleanupHook = null;
		}
	}

	public void blueprintEvent(BlueprintEvent event) {
		if (log.isTraceEnabled())
			log.trace("Scheduled background dispatch for blueprint event " + event);

		// schedule for execution right away
		eventDispatcher.schedule(new Task(event), 0);
	}
}