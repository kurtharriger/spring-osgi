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

import org.osgi.service.blueprint.reflect.CollectionBasedServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * @author Costin Leau
 */
class SpringCollectionBasedServiceReferenceComponentMetadata extends SpringServiceReferenceComponentMetadata implements
		CollectionBasedServiceReferenceComponentMetadata {

	private final Class<?> collectionType = null;
	private final Value comparator = null;
	private final int memberType = 0;
	private final int comparisonBasis = 0;


	/**
	 * Constructs a new
	 * <code>SpringCollectionBasedServiceReferenceComponentMetadata</code>
	 * instance.
	 * 
	 * @param name
	 * @param definition
	 */
	public SpringCollectionBasedServiceReferenceComponentMetadata(String name, BeanDefinition definition) {
		super(name, definition);

	}

	public Class<?> getCollectionType() {
		return collectionType;
	}

	public Value getComparator() {
		return comparator;
	}

	public int getMemberType() {
		return memberType;
	}

	public int getOrderingComparisonBasis() {
		return comparisonBasis;
	}
}