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

/**
 * Simple implementation for {@link ListValue} interface.
 * 
 * @author Costin Leau
 * 
 */
public class SimpleListValue implements ListValue {

	private final List<Value> list;


	/**
	 * Constructs a new <code>SimpleListValue</code> instance.
	 * 
	 * @param set
	 */
	public SimpleListValue(Value... values) {
		List<Value> vals = new ArrayList<Value>(values.length);
		Collections.addAll(vals, values);
		list = Collections.unmodifiableList(vals);
	}

	public List<Value> getList() {
		return list;
	}

	public String getValueType() {
		throw new UnsupportedOperationException();
	}
}
