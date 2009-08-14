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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Blueprint converter adapter for Spring 3.0 ConverterService.
 * 
 * @author Costin Leau
 */
public class SpringBlueprintConverterService implements ConversionService {

	/** fallback delegate */
	private final ConversionService delegate;
	private final List<Converter> converters = new ArrayList<Converter>();
	private final TypeConverter typeConverter;

	public SpringBlueprintConverterService() {
		this(null, null);
	}

	public SpringBlueprintConverterService(ConversionService delegate, ConfigurableBeanFactory cbf) {
		this.delegate = delegate;
		SimpleTypeConverter simpleTC = new SimpleTypeConverter();
		if (cbf != null) {
			cbf.copyRegisteredEditorsTo(simpleTC);
		}
		this.typeConverter = simpleTC;
	}

	public void add(Converter blueprintConverter) {
		synchronized (converters) {
			converters.add(blueprintConverter);
		}
	}

	public void add(Collection<Converter> blueprintConverters) {
		synchronized (converters) {
			converters.addAll(blueprintConverters);
		}
	}

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		if (targetType.isArray() || Collection.class.isAssignableFrom(targetType)
				|| Map.class.isAssignableFrom(targetType)) {
			return false;
		}
		return true;
	}

	public boolean canConvert(Class<?> sourceType, TypeDescriptor targetType) {
		Class<?> target = targetType.getType();
		if (targetType.isArray() || Collection.class.isAssignableFrom(target) || Map.class.isAssignableFrom(target)) {
			return false;
		}
		return true;
	}

	public <T> T convert(Object source, Class<T> targetType) {
		return (T) convert(source, TypeDescriptor.valueOf(targetType));
	}

	public Object convert(Object source, TypeDescriptor targetType) {
		ReifiedType type = TypeFactory.getType(targetType);
		synchronized (converters) {
			for (Converter converter : converters) {
				try {
					if (converter.canConvert(source, type)) {
						return converter.convert(source, type);
					}
				} catch (Exception ex) {
					throw new ConversionFailedException(source, source.getClass(), targetType.getType(), ex);
				}
			}
		}

		if (delegate != null) {
			delegate.convert(source, targetType);
		}

		return typeConverter.convertIfNecessary(source, targetType.getType(), targetType.getMethodParameter());
	}
}