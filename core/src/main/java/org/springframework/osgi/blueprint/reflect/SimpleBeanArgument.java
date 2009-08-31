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

import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.Metadata;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;

/**
 * Basic implementation for {@link BeanArgument} interface.
 * 
 * @author Costin Leau
 */
class SimpleBeanArgument implements BeanArgument {

	private final int index;
	private final String typeName;
	private final Metadata value;
	private static final int UNSPECIFIED_INDEX = -1;

	/**
	 * Constructs a new <code>SimpleBeanArgument</code> instance.
	 * 
	 * @param index
	 * @param typeName
	 * @param value
	 */
	public SimpleBeanArgument(int index, ValueHolder valueHolder) {
		this.index = index;
		this.typeName = valueHolder.getType();
		this.value = ValueFactory.buildValue(MetadataUtils.getValue(valueHolder));
	}

	public SimpleBeanArgument(ValueHolder valueHolder) {
		this(UNSPECIFIED_INDEX, valueHolder);
	}

	public int getIndex() {
		return index;
	}

	public Metadata getValue() {
		return value;
	}

	public String getValueType() {
		return typeName;
	}
}