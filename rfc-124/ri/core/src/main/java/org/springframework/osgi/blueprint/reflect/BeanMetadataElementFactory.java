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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.ListValue;
import org.osgi.service.blueprint.reflect.MapValue;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.PropertiesValue;
import org.osgi.service.blueprint.reflect.ReferenceNameValue;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.SetValue;
import org.osgi.service.blueprint.reflect.TypedStringValue;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedProperties;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.osgi.blueprint.config.internal.temporary.TempManagedList;
import org.springframework.osgi.blueprint.config.internal.temporary.TempManagedMap;
import org.springframework.osgi.blueprint.config.internal.temporary.TempManagedSet;

/**
 * Adapter between OSGi's Blueprint {@link Value} and Spring
 * {@link BeanMetadataElement}.
 * 
 * @see ValueFactory
 * @author Costin Leau
 */
class BeanMetadataElementFactory {

	static BeanMetadataElement buildBeanMetadata(Value value) {
		return buildBeanMetadata(value, null);
	}

	/**
	 * Creates the equivalent Spring metadata for the given value.
	 * 
	 * @param value
	 * @param defaultTypeName
	 * @return
	 */
	static BeanMetadataElement buildBeanMetadata(Value value, String defaultTypeName) {

		if (value instanceof ReferenceValue) {
			ReferenceValue reference = (ReferenceValue) value;
			return new RuntimeBeanReference(reference.getComponentName());
		}

		if (value instanceof ReferenceNameValue) {
			ReferenceNameValue reference = (ReferenceNameValue) value;
			return new RuntimeBeanNameReference(reference.getReferenceName());
		}

		if (value instanceof TypedStringValue) {
			TypedStringValue typedString = (TypedStringValue) value;
			String specifiedType = typedString.getTypeName();
			if (specifiedType == null) {
				specifiedType = defaultTypeName;
			}
			return new org.springframework.beans.factory.config.TypedStringValue(typedString.getStringValue(),
				specifiedType);
		}

		if (value instanceof NullValue) {
			return new org.springframework.beans.factory.config.TypedStringValue(null);
		}

		if (value instanceof ComponentValue) {
			ComponentValue component = (ComponentValue) value;
			return MetadataFactory.buildBeanDefinitionFor(component.getComponentMetadata());
		}

		if (value instanceof ListValue) {
			ListValue listValue = (ListValue) value;
			List<Value> list = (List<Value>) listValue.getList();
			String defaultType = listValue.getValueType();
			ManagedList managedList = new TempManagedList(list.size(), defaultType);

			for (Value val : list) {
				managedList.add(BeanMetadataElementFactory.buildBeanMetadata(val, defaultType));
			}
			return managedList;
		}

		if (value instanceof SetValue) {
			SetValue setValue = (SetValue) value;
			Set<Value> set = (Set<Value>) setValue.getSet();
			String defaultType = setValue.getValueType();

			ManagedSet managedSet = new TempManagedSet(set.size(), defaultType);

			for (Iterator<Value> iterator = set.iterator(); iterator.hasNext();) {
				Value val = iterator.next();
				managedSet.add(BeanMetadataElementFactory.buildBeanMetadata(val, defaultType));
			}

			return managedSet;
		}

		if (value instanceof MapValue) {
			MapValue mapValue = (MapValue) value;
			Map<Value, Value> map = (Map<Value, Value>) mapValue.getMap();
			String defaultKeyType = mapValue.getKeyType();
			String defaultValueType = mapValue.getValueType();
			ManagedMap managedMap = new TempManagedMap(map.size(), defaultKeyType, defaultValueType);
			Set<Entry<Value, Value>> entrySet = map.entrySet();

			for (Iterator<Entry<Value, Value>> iterator = entrySet.iterator(); iterator.hasNext();) {
				Entry<Value, Value> entry = iterator.next();
				managedMap.put(BeanMetadataElementFactory.buildBeanMetadata(entry.getKey(), defaultKeyType),
					BeanMetadataElementFactory.buildBeanMetadata(entry.getValue(), defaultValueType));
			}
		}

		if (value instanceof PropertiesValue) {
			PropertiesValue propertiesValue = (PropertiesValue) value;

			Properties properties = propertiesValue.getPropertiesValue();
			ManagedProperties managedProperties = new ManagedProperties();
			Set entrySet = managedProperties.entrySet();

			for (Iterator<Entry<Value, Value>> iterator = entrySet.iterator(); iterator.hasNext();) {
				Entry<Value, Value> entry = iterator.next();
				managedProperties.put(BeanMetadataElementFactory.buildBeanMetadata(entry.getKey()),
					BeanMetadataElementFactory.buildBeanMetadata(entry.getValue()));
			}
		}

		throw new IllegalArgumentException("Unknown value type " + value.getClass());
	}
}