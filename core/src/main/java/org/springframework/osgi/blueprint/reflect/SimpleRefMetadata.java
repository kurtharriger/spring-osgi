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

import org.osgi.service.blueprint.reflect.RefMetadata;

/**
 * Basic implementation for {@link ReferenceValue} interface.
 * 
 * @author Costin Leau
 * 
 */
public class SimpleRefMetadata implements RefMetadata {

	private final String componentId;

	/**
	 * Constructs a new <code>SimpleReferenceValue</code> instance.
	 * 
	 * @param id
	 */
	public SimpleRefMetadata(String id) {
		this.componentId = id;
	}

	public String getComponentId() {
		return componentId;
	}
}