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

import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * @author Costin Leau
 */
public class SpringRegistrationListenerMetadata implements RegistrationListenerMetadata {

	private static final String REG_PROP = "registrationMethod";
	private static final String UNREG_PROP = "unregistrationMethod";
	private static final String LISTENER_NAME_PROP = "targetBeanName";
	private static final String LISTENER_PROP = "target";

	private final Value listenerComponent;
	private final String registrationMethod, unregistrationMethod;


	public SpringRegistrationListenerMetadata(AbstractBeanDefinition beanDefinition) {
		MutablePropertyValues pvs = beanDefinition.getPropertyValues();
		registrationMethod = (String) MetadataUtils.getValue(pvs, REG_PROP);
		unregistrationMethod = (String) MetadataUtils.getValue(pvs, UNREG_PROP);

		// listener reference
		if (pvs.contains(LISTENER_NAME_PROP)) {
			listenerComponent = new SimpleReferenceValue((String) MetadataUtils.getValue(pvs, LISTENER_NAME_PROP));
		}
		else {
			// convert the BeanDefinitionHolder
			listenerComponent = ValueFactory.buildValue(MetadataUtils.getValue(pvs, LISTENER_PROP));
		}

	}

	public Value getListenerComponent() {
		return listenerComponent;
	}

	public String getRegistrationMethodName() {
		return registrationMethod;
	}

	public String getUnregistrationMethodName() {
		return unregistrationMethod;
	}
}