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

import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.TypedStringValue;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Simple implementation for {@link BindingListenerMetadata} interface.
 * 
 * @author Costin Leau
 */
public class SimpleBindingListenerMetadata implements BindingListenerMetadata {

	private static final String BIND_PROP = "bindMethod";
	private static final String UNBIND_PROP = "unbindMethod";
	private static final String LISTENER_NAME_PROP = "targetBeanName";
	private static final String LISTENER_PROP = "target";
	private final String bindMethodName, unbindMethodName;
	private final Value listenerComponent;


	public SimpleBindingListenerMetadata(AbstractBeanDefinition beanDefinition) {
		MutablePropertyValues pvs = beanDefinition.getPropertyValues();
		bindMethodName = (String) MetadataUtils.getValue(pvs, BIND_PROP);
		unbindMethodName = (String) MetadataUtils.getValue(pvs, UNBIND_PROP);

		// listener reference
		if (pvs.contains(LISTENER_NAME_PROP)) {
			listenerComponent = new SimpleReferenceValue((String) MetadataUtils.getValue(pvs, LISTENER_NAME_PROP));
		}
		else {
			// convert the BeanDefinitionHolder
			listenerComponent = ValueFactory.buildValue(MetadataUtils.getValue(pvs, LISTENER_PROP));
		}
	}

	/**
	 * Constructs a new <code>SimpleBindingListenerMetadata</code> instance.
	 * 
	 * @param bindMethodName
	 * @param unbindMethodName
	 * @param listenerComponent
	 */
	public SimpleBindingListenerMetadata(String bindMethodName, String unbindMethodName, Value listenerComponent) {
		this.bindMethodName = bindMethodName;
		this.unbindMethodName = unbindMethodName;
		this.listenerComponent = listenerComponent;
	}

	public String getBindMethodName() {
		return bindMethodName;
	}

	public Value getListenerComponent() {
		return listenerComponent;
	}

	public String getUnbindMethodName() {
		return unbindMethodName;
	}
}