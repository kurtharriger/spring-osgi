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

package org.springframework.osgi.blueprint.config.internal.temporary;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.util.ObjectUtils;

/**
 * Temporary extension to {@link TypedStringValue} that remembers the actual
 * specified value in the xml elements.
 * 
 * @author Costin Leau
 */
public class SpecifiedTypeStringValue extends TypedStringValue {

	private final String specifiedType;


	/**
	 * Constructs a new <code>SpecifiedTypeStringValue</code> instance.
	 * 
	 * @param value
	 * @param targetType
	 */
	public SpecifiedTypeStringValue(String value, Class targetType, String specifiedType) {
		super(value, targetType);
		this.specifiedType = specifiedType;
	}

	/**
	 * Constructs a new <code>SpecifiedTypeStringValue</code> instance.
	 * 
	 * @param value
	 * @param targetTypeName
	 */
	public SpecifiedTypeStringValue(String value, String targetTypeName, String specifiedType) {
		super(value, targetTypeName);
		this.specifiedType = specifiedType;
	}

	/**
	 * Constructs a new <code>SpecifiedTypeStringValue</code> instance.
	 * 
	 * @param value
	 */
	public SpecifiedTypeStringValue(String value) {
		super(value);
		this.specifiedType = null;
	}

	public boolean equals(Object other) {
		if (!(other instanceof SpecifiedTypeStringValue)) {
			return false;
		}
		SpecifiedTypeStringValue otherValue = (SpecifiedTypeStringValue) other;
		return super.equals(other) && ObjectUtils.nullSafeEquals(this.specifiedType, otherValue.specifiedType);
	}

	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(specifiedType) * 13 + super.hashCode();
	}

	public String toString() {
		return super.toString() + ", specified type [" + specifiedType + "]";
	}

	public String getSpecifiedType() {
		return specifiedType;
	}
}