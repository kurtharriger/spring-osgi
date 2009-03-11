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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.service.blueprint.reflect.SetValue;
import org.osgi.service.blueprint.reflect.Value;

/**
 * Simple implementation for {@link SetValue} interface.
 * 
 * @author Costin Leau
 */
public class SimpleSetValue implements SetValue {

	private final Set<Value> set;
	private final String valueType;


	/**
	 * Constructs a new <code>SimpleSetValue</code> instance.
	 * 
	 * @param set
	 */
	public SimpleSetValue(Value[] values, String valueType) {
		Set<Value> vals = new LinkedHashSet<Value>(values.length);
		Collections.addAll(vals, values);
		set = Collections.unmodifiableSet(vals);
		this.valueType = valueType;
	}

	public Set<Value> getSet() {
		return set;
	}

	public String getValueType() {
		return valueType;
	}
}