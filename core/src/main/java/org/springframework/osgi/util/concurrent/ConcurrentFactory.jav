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
package org.springframework.osgi.util.concurrent;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ClassUtils;

/**
 * Factory for concurrency utilities, being aware of backport-concurrent package
 * as well as of JDK 1.5+
 * 
 * <p/> Mainly for internal use within the framework.
 * 
 * <p>
 * The goal of this class is to avoid runtime dependencies on JDK 1.5+ or
 * backport-concurrent, simply using the best implementation that is available
 * at runtime.
 * 
 * JDK 1.5+ is preffered always over backport-concurrent.
 * 
 * @author Costin Leau
 * 
 */

// TODO: add jdk implementations
public abstract class ConcurrentFactory {

	private static final Log logger = LogFactory.getLog(ConcurrentFactory.class);

	/** Whether the backport-concurrent library is present on the classpath */
	private static final boolean backportConcurrentAvailable = ClassUtils.isPresent(
		"edu.emory.mathcs.backport.java.util.concurrent.locks.Lock", ConcurrentFactory.class.getClassLoader());

	/**
	 * Actual creation of backport-concurrent Collections. In separate inner
	 * class to avoid runtime dependency on the backport-concurrent library.
	 */
	private static abstract class BackportConcurrentCollectionFactory {

		private static Map createConcurrentHashMap(int initialCapacity) {
			return new edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap(initialCapacity);
		}
	}

	/**
	 * Actual creation of JDK 1.5+ concurrent Collections. In separate inner
	 * class to avoid runtime dependency on JDK 1.5.
	 */
	private static abstract class JdkConcurrentCollectionFactory {

//		private static Map createConcurrentHashMap(int initialCapacity) {
//			return new java.util.concurrent.ConcurrentHashMap(initialCapacity);
//		}
	}

}
