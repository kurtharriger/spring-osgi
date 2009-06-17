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

import java.util.List;

import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;

/**
 * Simple implementation for {@link MapValue} interface.
 * 
 * @author Costin Leau
 * 
 */
public class SimpleMapMetadata implements MapMetadata {

	private final List<MapEntry> entries;
	private final String keyValueType, valueValueType;

	/**
	 * 
	 * Constructs a new <code>SimpleMapMetadata</code> instance.
	 * 
	 * @param entries
	 * @param keyTypeName
	 * @param valueTypeName
	 */
	public SimpleMapMetadata(List<MapEntry> entries, String keyTypeName, String valueTypeName) {
		this.entries = entries;
		this.keyValueType = keyTypeName;
		this.valueValueType = valueTypeName;
	}

	public List<MapEntry> getEntries() {
		return entries;
	}

	public String getKeyType() {
		return keyValueType;
	}

	public String getValueType() {
		return valueValueType;
	}
}