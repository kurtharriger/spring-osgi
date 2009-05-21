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
import org.osgi.service.blueprint.reflect.PropsMetadata;

/**
 * Simple implementation for {@link PropertiesValue} interface.
 * 
 * @author Costin Leau
 */
public class SimplePropsMetadata implements PropsMetadata {

	private final List<MapEntry> entries;

	/**
	 * Constructs a new <code>SimplePropertiesValue</code> instance.
	 * 
	 * @param entries
	 */
	public SimplePropsMetadata(List<MapEntry> entries) {
		this.entries = entries;
	}

	public List<MapEntry> getEntries() {
		return entries;
	}
}
