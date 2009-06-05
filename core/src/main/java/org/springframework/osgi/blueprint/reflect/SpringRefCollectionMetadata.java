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

import org.osgi.service.blueprint.reflect.RefCollectionMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.osgi.service.importer.support.CollectionType;

/**
 * @author Costin Leau
 */
class SpringRefCollectionMetadata extends SpringServiceReferenceComponentMetadata implements RefCollectionMetadata {

	private static final String COLLECTION_PROP = "collectionType";
	private static final String COMPARATOR = "comparator";

	private final Class<?> collectionType;
	private final Target comparator;
	private final int memberType = -1;
	private final int orderingBasis = -1;

	/**
	 * Constructs a new <code>SpringRefCollectionMetadata</code> instance.
	 * 
	 * @param name
	 * @param definition
	 */
	public SpringRefCollectionMetadata(String name, BeanDefinition definition) {
		super(name, definition);

		MutablePropertyValues pvs = definition.getPropertyValues();
		// collection type
		CollectionType colType = (CollectionType) MetadataUtils.getValue(pvs, COLLECTION_PROP);
		collectionType = colType.getCollectionClass();
		Object comp = MetadataUtils.getValue(pvs, COMPARATOR);
		comparator = (comp != null ? (Target) ValueFactory.buildValue(comp) : null);
	}

	public Class<?> getCollectionType() {
		return collectionType;
	}

	public Target getComparator() {
		return comparator;
	}

	public int getMemberType() {
		throw new UnsupportedOperationException("member type not implemented yet");
	}

	public int getOrderingBasis() {
		throw new UnsupportedOperationException("member type not implemented yet");
	}
}