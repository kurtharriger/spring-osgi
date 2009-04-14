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

import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;

/**
 * Simple implementation for {@link ParameterSpecification} interface.
 * 
 * @author Costin Leau
 */
public class SimpleParameterSpecification implements ParameterSpecification {

	private final int index;
	private final String typeName;
	private final Value value;


	/**
	 * Constructs a new <code>SimpleParameterSpecification</code> instance.
	 * 
	 * @param index
	 * @param typeName
	 * @param value
	 */
	public SimpleParameterSpecification(int index, String typeName, Value value) {
		this.index = index;
		this.typeName = typeName;
		this.value = value;
	}

	public SimpleParameterSpecification(int index, ValueHolder valueHolder) {
		this.index = index;
		this.typeName = valueHolder.getType();
		this.value = ValueFactory.buildValue(MetadataUtils.getValue(valueHolder));
	}

	public int getIndex() {
		return index;
	}

	public String getTypeName() {
		return typeName;
	}

	public Value getValue() {
		return value;
	}
}