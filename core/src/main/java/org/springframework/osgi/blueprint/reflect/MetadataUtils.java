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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;

/**
 * Internal utility class for dealing with metadata.
 * 
 * @author Costin Leau
 * 
 */
abstract class MetadataUtils {

	static Object getValue(PropertyValues pvs, String name) {
		if (pvs.contains(name)) {
			PropertyValue pv = pvs.getPropertyValue(name);
			// return (pv.isConverted() ? pv.getConvertedValue() : pv.getValue());
			return pv.getValue();
		}

		return null;
	}

	static Object getValue(PropertyValue pv) {
		// return (pv.isConverted() ? pv.getConvertedValue() : pv.getValue());
		return pv.getValue();
	}

	static Object getValue(ValueHolder valueHolder) {
		// return (valueHolder.isConverted() ? valueHolder.getConvertedValue() : valueHolder.getValue());
		return valueHolder.getValue();
	}

	static List<BeanArgument> getBeanArguments(BeanDefinition definition) {
		List<BeanArgument> temp;

		ConstructorArgumentValues ctorValues = definition.getConstructorArgumentValues();

		// get indexed values
		Map<Integer, ValueHolder> indexedArguments = ctorValues.getIndexedArgumentValues();

		// check first the indexed arguments
		if (!indexedArguments.isEmpty()) {
			temp = new ArrayList<BeanArgument>(indexedArguments.size());

			for (Map.Entry<Integer, ValueHolder> entry : indexedArguments.entrySet()) {
				temp.add(new SimpleBeanArgument(entry.getKey(), entry.getValue()));
			}
		} else {
			// followed by the generic arguments
			List<ValueHolder> args = ctorValues.getGenericArgumentValues();
			temp = new ArrayList<BeanArgument>(args.size());
			for (ValueHolder valueHolder : args) {
				temp.add(new SimpleBeanArgument(valueHolder));
			}
		}

		return Collections.unmodifiableList(temp);
	}

	static List<BeanProperty> getBeanProperties(BeanDefinition definition) {
		List<BeanProperty> temp;

		List<PropertyValue> pvs = definition.getPropertyValues().getPropertyValueList();

		if (pvs.isEmpty()) {
			return Collections.<BeanProperty> emptyList();
		} else {
			temp = new ArrayList<BeanProperty>(pvs.size());
		}

		for (PropertyValue propertyValue : pvs) {
			temp.add(new SimpleBeanProperty(propertyValue));
		}

		return Collections.unmodifiableList(temp);
	}
}