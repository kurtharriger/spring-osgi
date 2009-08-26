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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
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
import org.springframework.osgi.context.support.internal.security.SecurityUtils;

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
	private final ConfigurableBeanFactory cbf;

	public SpringBlueprintConverterService() {
		this(null, null);
	}

	public SpringBlueprintConverterService(ConversionService delegate, ConfigurableBeanFactory cbf) {
		this.delegate = delegate;
		SimpleTypeConverter simpleTC = new SimpleTypeConverter();
		if (cbf != null) {
			cbf.copyRegisteredEditorsTo(simpleTC);
		}
		this.cbf = cbf;
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
		return canConvert(sourceType, TypeDescriptor.valueOf(targetType));
	}

	public boolean canConvert(Class<?> sourceType, TypeDescriptor targetType) {
		Class<?> target = targetType.getType();
		if (target != null
				&& (target.isArray() || Collection.class.isAssignableFrom(target) || Map.class.isAssignableFrom(target) || Dictionary.class
						.isAssignableFrom(target))) {
			return false;
		}
		return true;
	}

	public <T> T convert(Object source, Class<T> targetType) {
		return (T) convert(source, TypeDescriptor.valueOf(targetType));
	}

	public Object convert(final Object source, TypeDescriptor targetType) {
		final ReifiedType type = TypeFactory.getType(targetType);
		boolean hasSecurity = (System.getSecurityManager() != null);
		AccessControlContext acc = (hasSecurity ? SecurityUtils.getAccFrom(cbf) : null);
		Object result = null;

		if (hasSecurity) {
			result = AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					return doConvert(source, type);
				}
			}, acc);
		} else {
			result = doConvert(source, type);
		}

		if (result != null) {
			return result;
		}

		if (delegate != null) {
			delegate.convert(source, targetType);
		}

		Class<?> tType = targetType.getType();
//		if (Collection.class.isAssignableFrom(tType) || Map.class.isAssignableFrom(tType)
//				|| Dictionary.class.isAssignableFrom(tType)) {
//			tType = null;
//		}

		return typeConverter.convertIfNecessary(source, tType, targetType.getMethodParameter());
	}

	private Object doConvert(Object source, ReifiedType type) {
		synchronized (converters) {
			for (Converter converter : converters) {
				try {
					if (converter.canConvert(source, type)) {
						return converter.convert(source, type);
					}
				} catch (Exception ex) {
					throw new ConversionFailedException(source, source.getClass(), type.getRawClass(), ex);
				}
			}
		}
		return null;
	}
}