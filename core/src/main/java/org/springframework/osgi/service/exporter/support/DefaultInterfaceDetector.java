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
package org.springframework.osgi.service.exporter.support;

import org.springframework.osgi.util.internal.ClassUtils;

/**
 * Default implementation of {@link InterfaceDetector}.
 * 
 * @author Costin Leau
 */
public enum DefaultInterfaceDetector implements InterfaceDetector {

	/**
	 * Do not detect anything.
	 */
	DISABLED {
		private final Class<?>[] clazz = new Class[0];

		public Class<?>[] detect(Class<?> targetClass) {
			return clazz;
		}
	},

	INTERFACES {
		public Class<?>[] detect(Class<?> targetClass) {
			return ClassUtils.getClassHierarchy(targetClass, ClassUtils.ClassSet.INTERFACES);
		}
	},

	CLASS_HIERARCHY {
		public Class<?>[] detect(Class<?> targetClass) {
			return ClassUtils.getClassHierarchy(targetClass, ClassUtils.ClassSet.CLASS_HIERARCHY);
		}
	},

	ALL_CLASSES {
		public Class<?>[] detect(Class<?> targetClass) {
			return ClassUtils.getClassHierarchy(targetClass, ClassUtils.ClassSet.ALL_CLASSES);
		}
	}
}
