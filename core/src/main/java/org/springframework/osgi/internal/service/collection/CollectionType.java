/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.internal.service.collection;

import org.springframework.core.enums.StaticLabeledEnum;

/**
 * Enumeration-like class which indicates the supported OSGi service collection
 * types. This class is used mainly for configuration purposes (such as parsing
 * the OSGi namespace).
 * 
 * @author Costin Leau
 * 
 */
public class CollectionType extends StaticLabeledEnum {

	private static final long serialVersionUID = 320203314729289568L;

	/** unused */
	public static final CollectionType COLLECTION = new CollectionType(1, "collection", OsgiServiceCollection.class);

	/**
	 * List
	 */
	public static final CollectionType LIST = new CollectionType(2, "list", OsgiServiceList.class);

	/**
	 * Set.
	 */
	public static final CollectionType SET = new CollectionType(3, "set", OsgiServiceSet.class);

	/**
	 * Sorted List
	 */
	public static final CollectionType SORTED_LIST = new CollectionType(4, "sorted-list", OsgiServiceSortedList.class);

	/**
	 * Sorted Set.
	 */
	public static final CollectionType SORTED_SET = new CollectionType(5, "sorted-set", OsgiServiceSortedSet.class);

	private final Class collectionClass;

	/**
	 * Return the actual collection class used underneath.
	 * 
	 * @return collection class
	 */
	public Class getCollectionClass() {
		return collectionClass;
	}

	private CollectionType(int code, String label, Class collectionClass) {
		super(code, label);
		this.collectionClass = collectionClass;
	}
}
