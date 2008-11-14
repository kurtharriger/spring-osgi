/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.compendium.internal.cm;

import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;

/**
 * Utility class for the Configuration Admin package.
 * 
 * @author Costin Leau
 */
abstract class CMUtils {

	/**
	 * Injects the properties from the given Map to the given object.
	 * Additionally, a bean factory can be passed in for copying property
	 * editors inside the injector.
	 * 
	 * @param bean bean instance to configure
	 */
	public static void applyMapOntoInstance(Object instance, Map properties, AbstractBeanFactory beanFactory) {
		if (properties != null && !properties.isEmpty()) {
			BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(instance);
			// configure bean wrapper (using method from Spring 2.5.6)
			if (beanFactory != null) {
				beanFactory.copyRegisteredEditorsTo(beanWrapper);
			}
			for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry entry = (Map.Entry) iterator.next();
				String propertyName = (String) entry.getKey();

				if (beanWrapper.isWritableProperty(propertyName)) {
					beanWrapper.setPropertyValue(propertyName, entry.getValue());
				}
			}
		}
	}
}
