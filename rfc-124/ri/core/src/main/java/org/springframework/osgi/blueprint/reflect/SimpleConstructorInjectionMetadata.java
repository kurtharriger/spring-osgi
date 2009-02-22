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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.service.blueprint.reflect.ConstructorInjectionMetadata;
import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;

/**
 * Simple implementation for {@link ConstructorInjectionMetadata} interface.
 * 
 * @author Costin Leau
 */
public class SimpleConstructorInjectionMetadata implements ConstructorInjectionMetadata {

	private final List<ParameterSpecification> params;


	/**
	 * Constructs a new <code>SimpleConstructorInjectionMetadata</code>
	 * instance.
	 * 
	 * @param params
	 */
	public SimpleConstructorInjectionMetadata(List<ParameterSpecification> params) {
		this.params = params;
	}

	/**
	 * Constructs a new <code>SimpleConstructorInjectionMetadata</code>
	 * instance.
	 * 
	 * @param definition
	 */
	public SimpleConstructorInjectionMetadata(BeanDefinition definition) {
		ConstructorArgumentValues ctorValues = definition.getConstructorArgumentValues();

		// get indexed values
		Map<Integer, ValueHolder> indexedArguments = ctorValues.getIndexedArgumentValues();

		List<ParameterSpecification> temp = new ArrayList<ParameterSpecification>(indexedArguments.size());

		for (Iterator<Map.Entry<Integer, ValueHolder>> iterator = indexedArguments.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<Integer, ValueHolder> entry = iterator.next();
			temp.add(new SimpleParameterSpecification(entry.getKey(), entry.getValue()));
		}

		// get generic arguments
		List<ValueHolder> args = ctorValues.getGenericArgumentValues();
		for (ValueHolder arg : args) {
			temp.add(new SimpleParameterSpecification(-1, arg));
		}

		params = Collections.unmodifiableList(temp);
	}

	public List<ParameterSpecification> getParameterSpecifications() {
		return params;
	}
}