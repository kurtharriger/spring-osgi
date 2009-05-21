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

import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;

/**
 * @author Costin Leau
 */
class SpringReferenceMetadata extends SpringServiceReferenceComponentMetadata implements ReferenceMetadata {

	private static final String TIMEOUT_PROP = "timeout";
	private static final long DEFAULT_TIMEOUT = 300000;
	private final long timeout;

	/**
	 * Constructs a new <code>SpringUnaryServiceReferenceComponentMetadata</code> instance.
	 * 
	 * @param name
	 * @param definition
	 */
	public SpringReferenceMetadata(String name, BeanDefinition definition) {
		super(name, definition);

		MutablePropertyValues pvs = beanDefinition.getPropertyValues();
		if (pvs.contains(TIMEOUT_PROP)) {
			Object value = MetadataUtils.getValue(pvs, TIMEOUT_PROP);

			timeout = Long
					.parseLong((value instanceof String ? (String) value : ((TypedStringValue) value).getValue()));
		} else {
			timeout = DEFAULT_TIMEOUT;
		}
	}

	public long getTimeout() {
		return timeout;
	}
}