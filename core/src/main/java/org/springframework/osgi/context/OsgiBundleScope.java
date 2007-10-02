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
package org.springframework.osgi.context;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.util.Assert;

/**
 * Osgi bundle {@link org.springframework.beans.factory.config.Scope}
 * implementation.
 * 
 * Will allow per--calling-bundle object instance similar thus this scope
 * becomes useful when enabled on beans exposed as OSGi services.
 * 
 * @author Costin Leau
 * 
 */

// This class relies heavily on the OSGi ServiceFactory (SF) behaviour.
// Since the OSGi platform automatically calls get/ungetService on a SF
// and caches the getService() object there is no need for caching inside the
// scope.
// This also means that the scope cannot interact with the cache and acts
// only as an object creator and nothing more in favor of the ServiceFactory.
// However, note that for the inner bundle, the scope has to mimic the OSGi
// behavoir.
// 
public class OsgiBundleScope implements Scope, DisposableBean {

	public static final String SCOPE_NAME = "bundle";

	private static final Log log = LogFactory.getLog(OsgiBundleScope.class);

	/**
	 * Decorating {@link org.osgi.framework.ServiceFactory} used for supporting
	 * 'bundle' scoped beans.
	 * 
	 * @author Costin Leau
	 * 
	 */
	public static class BundleScopeServiceFactory implements ServiceFactory {
		private ServiceFactory decoratedServiceFactory;

		// FIXME: make this a concurrent map
		/** destruction callbacks for bean instances */
		private final Map callbacks = Collections.synchronizedMap(new LinkedHashMap(4));

		public BundleScopeServiceFactory(ServiceFactory serviceFactory) {
			Assert.notNull(serviceFactory);
			this.decoratedServiceFactory = serviceFactory;
		}

		/*
		 * Called if a bundle requests a service for the first time (start the
		 * scope).
		 * 
		 * (non-Javadoc)
		 * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle,
		 * org.osgi.framework.ServiceRegistration)
		 */
		public Object getService(Bundle bundle, ServiceRegistration registration) {
			try {
				// tell the scope, it's an outside bundle that does the call
				CALLING_BUNDLE.set(Boolean.TRUE);

				// create the new object
				Object obj = decoratedServiceFactory.getService(bundle, registration);

				// get callback (registered through the scope)
				Object passedObject = OsgiBundleScope.CALLING_BUNDLE.get();
				
				// make sure it's not the marker object
				if (passedObject != null && passedObject instanceof Runnable) {
					Runnable callback = (Runnable) OsgiBundleScope.CALLING_BUNDLE.get();
					if (callback != null)
						callbacks.put(bundle, callback);
				}
				return obj;
			}
			finally {
				// clean ThreadLocal
				OsgiBundleScope.CALLING_BUNDLE.set(null);
			}
		}

		/*
		 * Called if a bundle releases the service (stop the scope).
		 * 
		 * (non-Javadoc)
		 * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle,
		 * org.osgi.framework.ServiceRegistration, java.lang.Object)
		 */
		public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
			try {
				// tell the scope, it's an outside bundle that does the call
				CALLING_BUNDLE.set(Boolean.TRUE);
				// unget object first
				decoratedServiceFactory.ungetService(bundle, registration, service);

				// then apply the destruction callback (if any)
				Runnable callback = (Runnable) callbacks.get(bundle);
				if (callback != null)
					callback.run();
			}
			finally {
				// clean ThreadLocal
				CALLING_BUNDLE.set(null);
			}
		}

	}

	/**
	 * ThreadLocal used for passing objects around {@link OsgiBundleScope} and
	 * {@link BundleScopeServiceFactory} (there is only one scope instance but
	 * multiple BSSFs).
	 */
	public static final ThreadLocal CALLING_BUNDLE = new ThreadLocal();

	/**
	 * Map of beans imported by the current bundle from other bundles. This map
	 * is sychronized and is used by
	 * {@link org.springframework.osgi.context.OsgiBundleScope}.
	 */
	// TODO: replace this with a concurrent map
	private final Map beans = Collections.synchronizedMap(new LinkedHashMap(16));

	/**
	 * Unsynchronized map of callbacks for the services used by the running
	 * bundle.
	 * 
	 * Uses the bean name as key and as value, a list of callbacks associated
	 * with the bean instances.
	 */
	private final Map destructionCallbacks = new LinkedHashMap(8);

	private boolean isExternalBundleCalling() {
		return (CALLING_BUNDLE.get() != null);
	}

	public Object get(String name, ObjectFactory objectFactory) {
		// outside bundle calling (no need to cache things)
		if (isExternalBundleCalling()) {
			return objectFactory.getObject();
		}
		// in-appCtx call
		else {
			// use local bean repository
			synchronized (beans) {
				Object bean = beans.get(name);
				if (bean == null) {
					bean = objectFactory.getObject();
					beans.put(name, bean);
				}
				return bean;
			}
		}

	}

	public String getConversationId() {
		return null;
	}

	public void registerDestructionCallback(String name, Runnable callback) {
		// pass the destruction callback to the ServiceFactory
		if (isExternalBundleCalling())
			CALLING_BUNDLE.set(callback);
		// otherwise destroy the bean from the local cache
		else {
			destructionCallbacks.put(name, callback);
		}
	}

	/*
	 * Unable to do this as we cannot invalidate the OSGi cache.
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.Scope#remove(java.lang.String)
	 */
	public Object remove(String name) {
		throw new UnsupportedOperationException();
	}

	/*
	 * Clean up the scope (context refresh/close()).
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() {
		boolean debug = log.isDebugEnabled();
		
		// handle only the local cache/beans
		// the ServiceFactory object will be destroyed upon service unregistration
		for (Iterator iter = destructionCallbacks.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			Runnable callback = (Runnable) entry.getValue();

			if (debug)
				log.debug("destroying local bundle scoped bean [" + entry.getKey() + "]");
			
			callback.run();
		}

		destructionCallbacks.clear();
		beans.clear();
	}

}
