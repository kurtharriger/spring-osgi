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

import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.springframework.util.StringUtils;

/**
 * Simple implementation for {@link SimpleValueMetadata} interface. Understands Spring's
 * {@link org.springframework.beans.factory.config.TypedStringValue}.
 * 
 * @author Costin Leau
 * 
 */
class SimpleValueMetadata implements ValueMetadata {

	private final String typeName, value;

	/**
	 * Constructs a new <code>SimpleValueMetadata</code> instance.
	 * 
	 * @param typeName
	 * @param value
	 */
	public SimpleValueMetadata(String typeName, String value) {
		this.typeName = (StringUtils.hasText(typeName) ? typeName : null);
		this.value = value;
	}

	public SimpleValueMetadata(org.springframework.beans.factory.config.TypedStringValue typedStringValue) {
		String specifiedType = typedStringValue.getSpecifiedTypeName();
		this.typeName = (StringUtils.hasText(specifiedType) ? specifiedType : null);
		this.value = typedStringValue.getValue();
	}

	public String getStringValue() {
		return value;
	}

	public String getType() {
		return typeName;
	}
}