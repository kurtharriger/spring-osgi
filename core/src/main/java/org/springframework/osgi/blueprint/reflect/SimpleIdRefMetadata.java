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

import org.osgi.service.blueprint.reflect.IdRefMetadata;

/**
 * Simple implementation for {@link IdRefMetadata} interface.
 * 
 * @author Costin Leau
 * 
 */
class SimpleIdRefMetadata implements IdRefMetadata {

	private final String referenceName;

	/**
	 * Constructs a new <code>SimpleIdRefMetadata</code> instance.
	 * 
	 * @param name
	 */
	public SimpleIdRefMetadata(String name) {
		this.referenceName = name;
	}

	public String getComponentId() {
		return referenceName;
	}
}
