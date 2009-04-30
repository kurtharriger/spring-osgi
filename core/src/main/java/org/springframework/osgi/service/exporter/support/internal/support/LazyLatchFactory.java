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

package org.springframework.osgi.service.exporter.support.internal.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.Mergeable;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.core.io.Resource;

/**
 * Cache-like map used for retrieving synchronization latches for lazy exporters
 * inside an application context. The convention is that the Spring DM extender
 * populates the cache with latches while the exporters wait on them until the
 * application context is complete.
 * 
 * 
 * <b>Note:</b> to improve performance and avoid the use of reflection from the
 * extender side, this class implements public interfaces to wrap and expose its
 * methods. This is the only reason, the interfaces are used as the class does
 * not implement their contract or meaning.
 * 
 * @author Costin Leau
 */
public class LazyLatchFactory implements SourceExtractor, Mergeable {

	/** applicationContext -> CountDownLatch */
	private static final ConcurrentMap<Integer, CountDownLatch> cache = new ConcurrentHashMap<Integer, CountDownLatch>();


	public static CountDownLatch addLatch(Integer key) {
		return cache.putIfAbsent(key, new CountDownLatch(1));
	}

	public static CountDownLatch getLatch(Integer key) {
		return cache.get(key);
	}

	public static boolean hasLatch(Integer key) {
		return cache.containsKey(key);
	}

	public static CountDownLatch removeLatch(Integer key) {
		CountDownLatch latch = cache.remove(key);
		if (latch != null) {
			latch.countDown();
		}
		return latch;
	}

	public static void clear() {
		cache.clear();
	}

	// interface methods
	public Object extractSource(Object sourceCandidate, Resource definingResource) {
		return addLatch((Integer) sourceCandidate);
	}

	public Object merge(Object parent) {
		return removeLatch((Integer) parent);
	}

	public boolean isMergeEnabled() {
		clear();
		return false;
	}
}