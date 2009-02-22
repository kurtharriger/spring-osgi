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

import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
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
			return (String) (pv.isConverted() ? pv.getConvertedValue() : pv.getValue());
		}

		return null;
	}

	static Object getValue(ValueHolder valueHolder) {
		return (valueHolder.isConverted() ? valueHolder.getConvertedValue() : valueHolder.getValue());
	}
}
