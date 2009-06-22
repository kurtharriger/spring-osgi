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

import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.osgi.service.importer.support.MemberType;

/**
 * @author Costin Leau
 */
class SpringReferenceListMetadata extends SpringServiceReferenceComponentMetadata implements ReferenceListMetadata {

	private final int memberType;
	private static final String MEMBER_TYPE_PROP = "memberType";

	/**
	 * Constructs a new <code>SpringRefCollectionMetadata</code> instance.
	 * 
	 * @param name
	 * @param definition
	 */
	public SpringReferenceListMetadata(String name, BeanDefinition definition) {
		super(name, definition);

		MemberType type = MemberType.SERVICE_OBJECT;

		MutablePropertyValues pvs = beanDefinition.getPropertyValues();
		if (pvs.contains(MEMBER_TYPE_PROP)) {
			type = (MemberType) MetadataUtils.getValue(pvs, MEMBER_TYPE_PROP);
		}

		memberType = type.ordinal() + 1;
	}

	public int getMemberType() {
		return memberType;
	}
}