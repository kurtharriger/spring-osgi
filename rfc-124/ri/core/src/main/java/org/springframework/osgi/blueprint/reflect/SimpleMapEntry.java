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

import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;

/**
 * Basic MapEntry implementation.
 * 
 * @author Costin Leau
 */
class SimpleMapEntry implements MapEntry {

	private final NonNullMetadata key;
	private final Metadata value;

	public SimpleMapEntry(NonNullMetadata key, Metadata value) {
		this.key = key;
		this.value = value;
	}

	public NonNullMetadata getKey() {
		return key;
	}

	public Metadata getValue() {
		return value;
	}
}
