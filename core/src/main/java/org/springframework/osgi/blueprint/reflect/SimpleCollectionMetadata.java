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

import java.util.List;
import java.util.Set;

import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

/**
 * Basic {@link CollectionMetadata} implementation.
 * 
 * @author Costin Leau
 */
class SimpleCollectionMetadata implements CollectionMetadata {

	public enum CollectionType {
		ARRAY(Object[].class), LIST(List.class), SET(Set.class);

		private final Class<?> type;

		private CollectionType(Class<?> type) {
			this.type = type;
		}

		static CollectionType resolve(Class<?> type) {

			for (CollectionType supportedType : CollectionType.values()) {
				if (supportedType.type.equals(type)) {
					return supportedType;
				}
			}

			throw new IllegalArgumentException("Unsupported class type " + type);
		}
	}

	private final List<Metadata> values;
	private final CollectionType collectionType;
	private final String typeName;

	public SimpleCollectionMetadata(List<Metadata> values, CollectionType type, String valueTypeName) {
		this.values = values;
		this.collectionType = type;
		this.typeName = valueTypeName;
	}

	public SimpleCollectionMetadata(List<Metadata> values, Class<?> type, String valueTypeName) {
		this(values, CollectionType.resolve(type), valueTypeName);
	}

	public Class<?> getCollectionClass() {
		return collectionType.type;
	}

	public String getValueType() {
		return typeName;
	}

	public List<Metadata> getValues() {
		return values;
	}
}