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
import java.util.List;

import org.osgi.service.blueprint.reflect.ListValue;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.util.StringUtils;

/**
 * Simple implementation for {@link ListValue} interface.
 * 
 * @author Costin Leau
 * 
 */
public class SimpleListValue implements ListValue {

	private final List<Value> list;
	private final String valueType;


	/**
	 * Constructs a new <code>SimpleListValue</code> instance.
	 * 
	 * @param array of values
	 * @param valueType value type
	 */
	public SimpleListValue(Value[] values, String valueType) {
		List<Value> vals = new ArrayList<Value>(values.length);
		Collections.addAll(vals, values);
		list = Collections.unmodifiableList(vals);
		this.valueType = (StringUtils.hasText(valueType) ? valueType : null);
	}

	public List<Value> getList() {
		return list;
	}

	public String getValueType() {
		return valueType;
	}
}
