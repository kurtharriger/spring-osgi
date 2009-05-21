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

package org.springframework.osgi.blueprint.convert;

import org.osgi.service.blueprint.convert.ConversionService;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * Spring based {@link ConversionService} implementation. Uses the given bean
 * factory mechanism to perform the type conversion.
 * 
 * @author Costin Leau
 */
public class SpringConversionService implements ConversionService {

	private final ConfigurableBeanFactory beanFactory;


	/**
	 * Constructs a new <code>SpringConversionService</code> instance.
	 * 
	 * @param beanFactory bean factory used for performing type conversion
	 */
	public SpringConversionService(ConfigurableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public Object convert(Object fromValue, Class toType) throws Exception {
		TypeConverter typeConverter = beanFactory.getTypeConverter();
		return typeConverter.convertIfNecessary(fromValue, toType);
	}
}
