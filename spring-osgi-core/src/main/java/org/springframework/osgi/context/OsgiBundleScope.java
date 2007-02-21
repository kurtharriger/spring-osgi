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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.CollectionFactory;

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
public class OsgiBundleScope implements Scope, DisposableBean {

	public static final String SCOPE_NAME = "bundle";

	private static final Log log = LogFactory.getLog(OsgiBundleScope.class);

	/**
	 * Used by
	 * {@link org.springframework.osgi.service.OsgiServiceProxyFactoryBean} to
	 * indicate that a new bean has been requested. If set, it will contain an
	 * array of size 2 which will be populated by this call with the newly
	 * created bean and its destruction callback (if any).
	 */
	public static final ThreadLocal CALLING_BUNDLE = new ThreadLocal();

	/**
	 * Map of beans imported by the current bundle from other bundles. This map
	 * is sychronized and is used by
	 * {@link org.springframework.osgi.context.OsgiBundleScope}.
	 */
	private final Map beans = Collections.synchronizedMap(CollectionFactory.createLinkedMapIfPossible(16));

	/**
	 * Unsynchronized map of callbacks for the services used by the running
	 * bundle.
	 */
	private final Map destructionCallbacks = CollectionFactory.createLinkedMapIfPossible(8);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.Scope#get(java.lang.String,
	 * org.springframework.beans.factory.ObjectFactory)
	 */
	public Object get(String name, ObjectFactory objectFactory) {
		if (isExternalBundleCalling()) {
			return objectFactory.getObject();
		}
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

	protected boolean isExternalBundleCalling() {
		return (CALLING_BUNDLE.get() != null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.Scope#getConversationId()
	 */
	public String getConversationId() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.Scope#registerDestructionCallback(java.lang.String,
	 * java.lang.Runnable)
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		if (isExternalBundleCalling())
			CALLING_BUNDLE.set(callback);
		else {
			destructionCallbacks.put(name, callback);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.Scope#remove(java.lang.String)
	 */
	public Object remove(String name) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() {
		boolean debug = log.isDebugEnabled();
		for (Iterator iter = destructionCallbacks.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			if (debug)
				log.debug("destroying imported service [" + entry.getKey() + "]");

			((Runnable) entry.getValue()).run();
		}
		
		beans.clear();
		destructionCallbacks.clear();
	}

}
