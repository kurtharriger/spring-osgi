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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.osgi.service.blueprint.container.ReifiedType;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Factory between Spring type descriptor and reified type.
 * 
 * @author Costin Leau
 */
class TypeFactory {

	private static final GenericsReifiedType OBJECT = new GenericsReifiedType(Object.class);

	private static class GenericsReifiedType extends ReifiedType {

		private final List<ReifiedType> arguments;
		private final int size;

		GenericsReifiedType(Class<?> clazz) {
			this(TypeDescriptor.valueOf(clazz));
		}

		GenericsReifiedType(TypeDescriptor descriptor) {
			super(ClassUtils.resolvePrimitiveIfNecessary(descriptor.getType()));
			arguments = getArguments(descriptor);
			size = arguments.size();
		}

		@Override
		public ReifiedType getActualTypeArgument(int i) {
			if (i >= 0 && i < size) {
				return arguments.get(i);
			}
			if (i == 0) {
				return super.getActualTypeArgument(0);
			}

			throw new IllegalArgumentException("Invalid argument index given " + i);
		}

		@Override
		public int size() {
			return size;
		}
	};

	static ReifiedType getType(TypeDescriptor targetType) {
		return new GenericsReifiedType(targetType);
	}

	private static List<ReifiedType> getArguments(TypeDescriptor type) {
		List<ReifiedType> arguments;

		// is it an array/map
		if (type.isCollection()) {
			arguments = new ArrayList<ReifiedType>(1);
			Class<?> elementType = type.getElementType();
			arguments.add(elementType != null ? new GenericsReifiedType(elementType) : OBJECT);
			return arguments;
		}

		if (type.isMap()) {
			arguments = new ArrayList<ReifiedType>(2);
			Class<?> elementType = type.getMapKeyType();
			arguments.add(elementType != null ? new GenericsReifiedType(elementType) : OBJECT);
			elementType = type.getMapValueType();
			arguments.add(elementType != null ? new GenericsReifiedType(elementType) : OBJECT);
			return arguments;
		}

		// check if the class is parameterized
		MethodParameter mp = type.getMethodParameter();
		if (mp != null) {
			Type targetType = GenericTypeResolver.getTargetType(mp);
			if (!(targetType instanceof Class)) {
				ReifiedType rType = getReifiedType(targetType);
				arguments = new ArrayList<ReifiedType>(1);
				arguments.add(rType);
				return arguments;
			}
		}

		return Collections.emptyList();
	}

	private static ReifiedType getReifiedType(Type targetType) {
		if (targetType instanceof Class) {
			if (Object.class.equals(targetType)) {
				return OBJECT;
			}
			return new GenericsReifiedType((Class<?>) targetType);
		}
		if (targetType instanceof ParameterizedType) {
			Type ata = ((ParameterizedType) targetType).getActualTypeArguments()[0];
			return getReifiedType(ata);
		}
		if (targetType instanceof WildcardType) {
			WildcardType wt = (WildcardType) targetType;
			Type[] lowerBounds = wt.getLowerBounds();
			if (ObjectUtils.isEmpty(lowerBounds)) {
				// there's always an upper bound (Object)
				Type upperBound = wt.getUpperBounds()[0];
				return getReifiedType(upperBound);
			}

			return getReifiedType(lowerBounds[0]);
		}

		if (targetType instanceof TypeVariable) {
			Type[] bounds = ((TypeVariable) targetType).getBounds();
			return getReifiedType(bounds[0]);
		}

		if (targetType instanceof GenericArrayType) {
			return getReifiedType(((GenericArrayType) targetType).getGenericComponentType());
		}

		throw new IllegalArgumentException("Unknown type " + targetType.getClass());
	}
}