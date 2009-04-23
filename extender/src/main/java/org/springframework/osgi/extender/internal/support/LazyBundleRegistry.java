/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.extender.internal.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.osgi.framework.Bundle;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Map-like class with dedicated support for lazy bundles. Allows execution of
 * operations on active bundles, promoting the lazy ones, if necessary, as fall
 * back.
 * 
 * The class is thread-safe.
 * 
 * @author Costin Leau
 * @param <T> the entity type associated with active bundles
 */
/**
 * @author Costin Leau
 * @param <T>
 */
class LazyBundleRegistry<T> {

	/** logger */
	private final Log log;


	/**
	 * A simple condition-like class.
	 * 
	 * @author Costin Leau
	 */
	interface Condition {

		/**
		 * Indicates if the given target bundle passes the condition or not.
		 * 
		 * @param bundle target bundle
		 * @return true if the bundle passes the condition, false otherwise
		 */
		boolean pass(Bundle bundle);
	}

	/**
	 * Activator action performed on lazy bundles upon promotion.
	 * 
	 * @author Costin Leau
	 * @param <V>
	 */
	interface Activator<V> {

		/**
		 * Activates the given target bundle.
		 * 
		 * @param <V> inherited return type
		 * @param bundle target bundle
		 * @return the bundle associated value
		 */
		V activate(Bundle bundle);
	}

	/**
	 * Operation performed all bundles - first active ones, followed by lazy
	 * ones (if nothing is found (null is returned)) by 'activating' them.
	 * 
	 * @author Costin Leau
	 * @param <T>
	 * @param <V>
	 */
	interface Operation<T, V> {

		/**
		 * Acts upon the bundle associated object (given as argument).
		 * 
		 * @param object associated object
		 * @return the result of the operation
		 */
		V operate(T t) throws Exception;
	}


	/** active, valid bundles */
	private final ConcurrentMap<Bundle, T> activeBundles = new ConcurrentHashMap<Bundle, T>(8);
	/** lazy bundles (potentially invalid) */
	private final ConcurrentMap<Bundle, Object> lazyBundles = new ConcurrentHashMap<Bundle, Object>(8);
	/** dummy value for concurrent hash maps */
	private static final Object VALUE = new Object();

	/**
	 * Queue of bundles that have been activated and validated and should be
	 * removed from the lazy map. This is needed so that promoted bundles do not
	 * go unseen by threads using the method at that point.
	 */
	private final List<Bundle> promotionQueue = new ArrayList<Bundle>(4);

	/** counter used for determining the promotion thread */
	private volatile AtomicInteger threadCounter = new AtomicInteger();

	private final Condition condition;
	private final Activator<T> activator;


	LazyBundleRegistry(Condition promotionCondition, Activator<T> activator, Log log) {
		this.condition = promotionCondition;
		this.activator = activator;
		this.log = log;
	}

	void add(Bundle bundle, boolean isLazy) {
		if (isLazy) {
			lazyBundles.put(bundle, VALUE);
		}
		else {
			activeBundles.put(bundle, activator.activate(bundle));
		}
	}

	boolean remove(Bundle bundle) {
		boolean value = false;

		value = (activeBundles.remove(bundle) != null);
		value |= (lazyBundles.remove(bundle) != null);

		synchronized (promotionQueue) {
			value |= promotionQueue.remove(bundle);
		}

		return value;
	}

	/**
	 * Applies an operation on all the bundles. To cope with concurrent
	 * environment, the class uses several queues so that lazy bundles that get
	 * activated, do not get ignored by threads hitting the method right at that
	 * point.
	 * 
	 * @param <V>
	 * @param action
	 * @return
	 */
	<V> V apply(Operation<T, V> action) throws Exception {
		boolean debug = log.isDebugEnabled();

		// count running thread
		threadCounter.incrementAndGet();

		try {
			// check active bundles
			for (Iterator<T> i = activeBundles.values().iterator(); i.hasNext();) {
				T result = i.next();
				V value = action.operate(result);
				if (value != null) {
					return value;
				}
			}

			// nothing found, look into lazy bundles 
			for (Iterator<Bundle> i = lazyBundles.keySet().iterator(); i.hasNext();) {
				Bundle bundle = i.next();

				if (condition.pass(bundle)) {
					// promote bundles
					T result = activeBundles.putIfAbsent(bundle, activator.activate(bundle));
					if (result == null) {
						synchronized (promotionQueue) {
							promotionQueue.add(bundle);
						}
					}
					else {
						result = activeBundles.get(bundle);
					}
					if (result != null) {
						V value = action.operate(result);
						if (value != null) {
							return value;
						}
					}
				}
				else {
					// the bundle is not compatible, remove it asap  
					lazyBundles.remove(bundle);
					if (debug)
						log.debug("Activated lazy bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle)
								+ " but found it unsuitable");
				}
			}

			// nothing found
			return null;
		}
		// cleanup the promotion queue
		finally {
			Bundle[] loggedBundles = null;

			synchronized (promotionQueue) {
				// the last thread accessing the method, does the cleanup
				// this way, we know other threads will pick the most up to date state
				// of the concurrent map
				if (threadCounter.decrementAndGet() == 0) {
					for (Bundle bundle : promotionQueue) {
						// remove bundle from the lazy map
						// but check that it wasn't removed in the meantime
						// by #remove(Bundle)
						if (lazyBundles.remove(bundle) == null) {
							activeBundles.remove(bundle);
						}
					}
				}
				if (debug && !promotionQueue.isEmpty()) {
					loggedBundles = (Bundle[]) promotionQueue.toArray(new Bundle[promotionQueue.size()]);
				}
				promotionQueue.clear();
			}

			if (loggedBundles != null) {
				StringBuilder builder = new StringBuilder("Activated (and validated) lazy bundles [ ");
				for (Bundle bundle : loggedBundles) {
					builder.append(OsgiStringUtils.nullSafeNameAndSymName(bundle));
					builder.append(" ");
				}
				builder.append("]");
				log.debug(builder);
			}
		}
	}

	public void clear() {
		promotionQueue.clear();
		lazyBundles.clear();
		activeBundles.clear();
	}
}