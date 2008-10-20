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

import org.osgi.service.blueprint.reflect.TypedStringValue;

/**
 * @author acolyer
 *
 */
public class TypedStringValueObject implements TypedStringValue {
	
	private final String value;
	private final String typeName;
	
	public TypedStringValueObject(String value, String typeName) {
		if (null == value) {
			throw new IllegalArgumentException("value cannot be null");
		}
		if (null == typeName) {
			throw new IllegalArgumentException("type name cannot be null");
		}
		
		this.value = value;
		this.typeName = typeName;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.TypedStringValue#getStringValue()
	 */
	public String getStringValue() {
		return this.value;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.TypedStringValue#getTypeName()
	 */
	public String getTypeName() {
		return this.typeName;
	}

}
