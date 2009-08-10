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

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * Blueprint converter exposing the backing container conversion capabilities.
 * 
 * @author Costin Leau
 */
public class SpringBlueprintConverter implements Converter {

	private final ConfigurableBeanFactory beanFactory;
	private volatile TypeConverter typeConverter;

	public SpringBlueprintConverter(ConfigurableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public boolean canConvert(Object source, ReifiedType targetType) {
		Class<?> required = targetType.getRawClass();
		try {
			getConverter().convertIfNecessary(source, required);
			return true;
		} catch (TypeMismatchException ex) {
			return false;
		}
	}

	public Object convert(Object source, ReifiedType targetType) throws Exception {
		Class<?> target = (targetType != null ? targetType.getRawClass() : null);
		return getConverter().convertIfNecessary(source, target);
	}

	private TypeConverter getConverter() {
		if (typeConverter == null) {
			SimpleTypeConverter simpleConverter = new SimpleTypeConverter();
			beanFactory.copyRegisteredEditorsTo(simpleConverter);
			simpleConverter.setConversionService(beanFactory.getConversionService());
			typeConverter = simpleConverter;
		}
		return typeConverter;
	}
}
