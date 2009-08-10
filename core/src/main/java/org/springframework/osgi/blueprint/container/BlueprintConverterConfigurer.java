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

import java.util.List;

import org.osgi.service.blueprint.container.Converter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.core.convert.ConversionService;

/**
 * Dedicated class for registering (in a declarative way) the adapter between Blueprint and Spring 3.0 converters.
 * 
 * @author Costin Leau
 */
public class BlueprintConverterConfigurer implements BeanFactoryAware {

	private final List<Converter> converters;

	public BlueprintConverterConfigurer(List<Converter> converters) {
		this.converters = converters;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof AbstractBeanFactory) {
			AbstractBeanFactory bf = ((AbstractBeanFactory) beanFactory);
			ConversionService cs = bf.getConversionService();
			SpringBlueprintConverterService sbc = new SpringBlueprintConverterService(cs, bf);
			sbc.add(converters);
			bf.setConversionService(sbc);
		}
	}
}
