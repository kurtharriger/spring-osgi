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
import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.blueprint.reflect.MapValue;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.util.Assert;

/**
 * Simple implementation for {@link MapValue} interface.
 * 
 * @author Costin Leau
 * 
 */
public class SimpleMapValue implements MapValue {

	private final Map<Value, Value> map;


	/**
	 * Constructs a new <code>SimpleListValue</code> instance.
	 * 
	 * @param set
	 */
	public SimpleMapValue(Value[] keys, Value[] values) {
		Assert.state(keys.length == values.length, "the keys and values arrays must of the same length");
		Map<Value, Value> vals = new LinkedHashMap<Value, Value>(keys.length);
		for (int i = 0; i < keys.length; i++) {
			vals.put(keys[i], values[i]);
		}
		map = Collections.unmodifiableMap(vals);
	}

	public Map<Value, Value> getMap() {
		return map;
	}

	public String getKeyType() {
		throw new UnsupportedOperationException();
	}

	public String getValueType() {
		throw new UnsupportedOperationException();
	}
}
