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

package org.springframework.osgi.extender.internal.support;

import java.util.concurrent.CountDownLatch;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.Mergeable;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.util.ClassUtils;

/**
 * Delegate class that allows access to LazyLatchFactory class (inside Spring DM
 * core) from Spring DM extender.
 * 
 * @author Costin Leau
 */
public abstract class LazyLatchFactoryDelegate {

	private static final Mergeable a;
	private static final SourceExtractor b;

	static {
		ClassLoader extenderClassLoader = LazyLatchFactoryDelegate.class.getClassLoader();
		Class<?> coreClass = ClassUtils.resolveClassName("org.springframework.osgi.util.OsgiBundleUtils",
			extenderClassLoader);
		Class<?> clzz = ClassUtils.resolveClassName(
			"org.springframework.osgi.service.exporter.support.internal.support.LazyLatchFactory",
			coreClass.getClassLoader());
		Object factory = BeanUtils.instantiateClass(clzz);
		a = (Mergeable) factory;
		b = (SourceExtractor) factory;
	}


	public static CountDownLatch addLatch(Integer key) {
		return (CountDownLatch) b.extractSource(key, null);
	}

	public static CountDownLatch removeLatch(Integer key) {
		return (CountDownLatch) a.merge(key);
	}

	public static void clear() {
		a.isMergeEnabled();
	}
}
