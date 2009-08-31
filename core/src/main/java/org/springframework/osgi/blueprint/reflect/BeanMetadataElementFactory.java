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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedArray;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedProperties;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.osgi.blueprint.reflect.internal.support.OrderedManagedProperties;

/**
 * Adapter between OSGi's Blueprint {@link Value} and Spring {@link BeanMetadataElement}.
 * 
 * @see ValueFactory
 * @author Costin Leau
 */
class BeanMetadataElementFactory {

	static BeanMetadataElement buildBeanMetadata(Metadata metadata) {
		return buildBeanMetadata(metadata, null);
	}

	/**
	 * Creates the equivalent Spring metadata for the given value.
	 * 
	 * @param value
	 * @param defaultTypeName
	 * @return
	 */
	static BeanMetadataElement buildBeanMetadata(Metadata value, String defaultTypeName) {

		if (value instanceof ValueMetadata) {
			ValueMetadata typedString = (ValueMetadata) value;
			String specifiedType = typedString.getType();
			if (specifiedType == null) {
				specifiedType = defaultTypeName;
			}
			return new org.springframework.beans.factory.config.TypedStringValue(typedString.getStringValue(),
					specifiedType);
		}

		if (value instanceof ComponentMetadata) {
			ComponentMetadata component = (ComponentMetadata) value;
			return MetadataFactory.buildBeanDefinitionFor(component);
		}

		// null vs non-null check
		if (value instanceof NullMetadata) {
			return new org.springframework.beans.factory.config.TypedStringValue(null);
		}

		if (value instanceof RefMetadata) {
			RefMetadata reference = (RefMetadata) value;
			return new RuntimeBeanReference(reference.getComponentId());
		}

		if (value instanceof IdRefMetadata) {
			IdRefMetadata reference = (IdRefMetadata) value;
			return new RuntimeBeanNameReference(reference.getComponentId());
		}

		if (value instanceof CollectionMetadata) {
			CollectionMetadata collection = (CollectionMetadata) value;

			Class<?> type = collection.getCollectionClass();
			List<Metadata> values = collection.getValues();

			Collection coll;
			if (List.class.isAssignableFrom(type)) {
				ManagedList<BeanMetadataElement> list = new ManagedList<BeanMetadataElement>(values.size());
				list.setElementTypeName(collection.getValueType());
				coll = list;
			} else if (Set.class.isAssignableFrom(type)) {
				ManagedSet<BeanMetadataElement> set = new ManagedSet<BeanMetadataElement>(values.size());
				set.setElementTypeName(collection.getValueType());
				coll = set;
			} else if (Object[].class.isAssignableFrom(type)) {
				ManagedArray array = new ManagedArray(collection.getValueType(), values.size());
				coll = array;
			} else {
				throw new IllegalArgumentException("Cannot create collection for type " + type);
			}

			for (Metadata val : values) {
				coll.add(BeanMetadataElementFactory.buildBeanMetadata(val, collection.getValueType()));
			}
			return (BeanMetadataElement) coll;
		}

		if (value instanceof MapMetadata) {
			MapMetadata mapValue = (MapMetadata) value;
			List<MapEntry> entries = mapValue.getEntries();
			String defaultKeyType = mapValue.getKeyType();
			String defaultValueType = mapValue.getValueType();

			ManagedMap<BeanMetadataElement, BeanMetadataElement> managedMap =
					new ManagedMap<BeanMetadataElement, BeanMetadataElement>(entries.size());
			managedMap.setKeyTypeName(defaultKeyType);
			managedMap.setValueTypeName(defaultValueType);

			for (MapEntry mapEntry : entries) {
				managedMap.put(BeanMetadataElementFactory.buildBeanMetadata(mapEntry.getKey(), defaultKeyType),
						BeanMetadataElementFactory.buildBeanMetadata(mapEntry.getValue(), defaultValueType));

			}

			return managedMap;
		}

		if (value instanceof PropsMetadata) {
			PropsMetadata propertiesValue = (PropsMetadata) value;

			List<MapEntry> entries = propertiesValue.getEntries();
			ManagedProperties managedProperties = new OrderedManagedProperties();

			for (MapEntry mapEntry : entries) {
				managedProperties.put(BeanMetadataElementFactory.buildBeanMetadata(mapEntry.getKey()),
						BeanMetadataElementFactory.buildBeanMetadata(mapEntry.getValue()));
			}
		}

		throw new IllegalArgumentException("Unknown value type " + value.getClass());
	}
}