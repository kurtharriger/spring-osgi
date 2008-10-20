/*
 * Copyright 2008 the original author or authors.
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

import org.osgi.service.blueprint.reflect.Value;

public abstract class NameValueInjectionMetadata {

	private final String name;
	private Value value;

	public NameValueInjectionMetadata(String name, Value v) {
		if (null == name) {
			throw new IllegalArgumentException("name cannot be null");
		}
		if (null == v) {
			throw new IllegalArgumentException("value cannot be null");
		}
		this.name = name;
		this.value = v;
	}

	public String getName() {
		return this.name;
	}

	public Value getValue() {
		return this.value;
	}

	public void setValue(Value v) {
		if (null == v) {
			throw new IllegalArgumentException("value cannot be null");
		}
		this.value = v;
	}

}