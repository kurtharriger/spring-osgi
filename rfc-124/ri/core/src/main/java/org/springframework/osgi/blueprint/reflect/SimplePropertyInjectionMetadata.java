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

import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.PropertyValue;

/**
 * Simple implementation for {@link PropertyInjectionMetadata} interface.
 * 
 * @author Costin Leau
 * 
 */
public class SimplePropertyInjectionMetadata implements PropertyInjectionMetadata {

	private final String name;
	private final Value value;


	/**
	 * Constructs a new <code>SimplePropertyInjectionMetadata</code> instance.
	 * 
	 * @param name
	 * @param value
	 */
	public SimplePropertyInjectionMetadata(String name, Value value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Constructs a new <code>SimplePropertyInjectionMetadata</code> instance.
	 * 
	 * @param propertyValue
	 */
	public SimplePropertyInjectionMetadata(PropertyValue propertyValue) {
		this.name = propertyValue.getName();
		Object value = (propertyValue.isConverted() ? propertyValue.getConvertedValue() : propertyValue.getValue());
		this.value = ValueFactory.buildValue(value);
	}

	public String getName() {
		return name;
	}

	public Value getValue() {
		return value;
	}
}
