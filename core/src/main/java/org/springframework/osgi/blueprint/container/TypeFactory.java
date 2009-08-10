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
package org.springframework.osgi.blueprint.container;

import org.osgi.service.blueprint.container.ReifiedType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ClassUtils;

/**
 * Factory between Spring type descriptor and reified type.
 * 
 * @author Costin Leau
 */
class TypeFactory {

	private static class GenericsReifiedType extends ReifiedType {

		public GenericsReifiedType(Class<?> clazz) {
			super(clazz);
		}

		@Override
		public ReifiedType getActualTypeArgument(int i) {
			return super.getActualTypeArgument(i);
		}

		@Override
		public Class getRawClass() {
			return super.getRawClass();
		}

		@Override
		public int size() {
			return super.size();
		}
	};

	static ReifiedType getType(TypeDescriptor targetType) {
		return new ReifiedType(ClassUtils.resolvePrimitiveIfNecessary(targetType.getType()));
	}
}
