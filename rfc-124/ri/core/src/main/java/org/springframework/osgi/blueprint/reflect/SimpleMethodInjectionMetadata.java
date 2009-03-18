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

package org.springframework.osgi.blueprint.reflect;

import java.util.List;

import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Simple implementation for {@link MethodInjectionMetadata} interface.
 * 
 * @author Costin Leau
 * 
 */
public class SimpleMethodInjectionMetadata implements MethodInjectionMetadata {

	private final String name;
	private final List<ParameterSpecification> params;


	/**
	 * Constructs a new <code>SimpleMethodInjectionMetadata</code> instance.
	 * 
	 * @param name
	 * @param params
	 */
	public SimpleMethodInjectionMetadata(String name, List<ParameterSpecification> params) {
		this.name = name;
		this.params = params;
	}

	/**
	 * Constructs a new <code>SimpleMethodInjectionMetadata</code> instance.
	 * 
	 * @param definition
	 */
	public SimpleMethodInjectionMetadata(BeanDefinition definition) {
		this.name = definition.getFactoryMethodName();

		params = MetadataUtils.getParameterList(definition.getConstructorArgumentValues());
	}

	public String getName() {
		return name;
	}

	public List<ParameterSpecification> getParameterSpecifications() {
		return params;
	}
}