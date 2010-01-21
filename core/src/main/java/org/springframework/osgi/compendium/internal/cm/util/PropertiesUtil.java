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
package org.springframework.osgi.compendium.internal.cm.util;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.springframework.util.CollectionUtils;

/**
 * @author Costin Leau
 */
public class PropertiesUtil {

	/**
	 * Merges the given map into the target properties object. Additionally it checks if there are any given local
	 * properties and whether these can override the source.
	 * 
	 * @return a new (Properties) object mergeing the local properties and the source
	 */
	public static Properties initProperties(Properties localMap, boolean localOverride, Map<?, ?> source,
			Properties target) {

		synchronized (target) {
			target.clear();

			// merge the local properties (upfront)
			if (localMap != null && !localOverride) {
				CollectionUtils.mergePropertiesIntoMap(localMap, target);
			}

			if (source != null) {
				target.putAll(source);
			}

			// merge local properties (if needed)
			if (localMap != null && localOverride) {
				CollectionUtils.mergePropertiesIntoMap(localMap, target);
			}

			return target;
		}
	}

	/**
	 * Merges the given dictionary into the target properties object. Additionally it checks if there are any given
	 * local properties and whether these can override the source. Identical to
	 * {@link #initProperties(Properties, boolean, Map, Properties)} excepts it reads the dictionary directly to avoid
	 * any mapping overhead.
	 * 
	 * @param localMap
	 * @param localOverride
	 * @param source
	 * @param target
	 * @return
	 */
	public static Properties initProperties(Properties localMap, boolean localOverride, Dictionary source,
			Properties target) {

		synchronized (target) {
			target.clear();

			// merge the local properties (upfront)
			if (localMap != null && !localOverride) {
				CollectionUtils.mergePropertiesIntoMap(localMap, target);
			}

			if (source != null) {
				Enumeration<?> keys = source.keys();
				for (; keys.hasMoreElements();) {
					Object key = keys.nextElement();
					Object value = source.get(key);
					if (key != null && value != null) {
						target.put(key, value);
					}
				}
			}

			// merge local properties (if needed)
			if (localMap != null && localOverride) {
				CollectionUtils.mergePropertiesIntoMap(localMap, target);
			}

			return target;
		}
	}
}