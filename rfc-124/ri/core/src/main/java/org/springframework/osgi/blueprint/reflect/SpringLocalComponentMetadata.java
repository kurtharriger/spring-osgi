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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.service.blueprint.reflect.ConstructorInjectionMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.StringUtils;

/**
 * Default {@link LocalComponentMetadata} implementation based on Spring's
 * {@link BeanDefinition}.
 * 
 * @author Costin Leau
 */
class SpringLocalComponentMetadata extends SpringComponentMetadata implements LocalComponentMetadata {

	private final ConstructorInjectionMetadata constructorMetadata;
	private final MethodInjectionMetadata factoryMethodMetadata;
	private final Collection<PropertyInjectionMetadata> propertyMetadata;
	private final Value factoryComponent;


	/**
	 * Constructs a new <code>SpringLocalComponentMetadata</code> instance.
	 * 
	 * @param name bean name
	 * @param definition Spring bean definition
	 */
	public SpringLocalComponentMetadata(String name, BeanDefinition definition) {
		super(name, definition);

		final String factoryMethod = definition.getFactoryMethodName();
		if (StringUtils.hasText(factoryMethod)) {
			factoryMethodMetadata = new SimpleMethodInjectionMetadata(definition);
			String factory = definition.getFactoryBeanName();
			if (StringUtils.hasText(factory)) {
				factoryComponent = new SimpleReferenceValue(factory);
			}
			else {
				factoryComponent = null;
			}
		}
		else {
			factoryComponent = null;
			factoryMethodMetadata = null;
		}

		constructorMetadata = new SimpleConstructorInjectionMetadata(definition);

		List<PropertyValue> pvs = definition.getPropertyValues().getPropertyValueList();
		List<PropertyInjectionMetadata> props = new ArrayList<PropertyInjectionMetadata>(pvs.size());

		for (PropertyValue propertyValue : pvs) {
			props.add(new SimplePropertyInjectionMetadata(propertyValue));
		}

		propertyMetadata = Collections.unmodifiableCollection(props);
	}

	public String getClassName() {
		return beanDefinition.getBeanClassName();
	}

	public ConstructorInjectionMetadata getConstructorInjectionMetadata() {
		return constructorMetadata;
	}

	public String getDestroyMethodName() {
		return beanDefinition.getInitMethodName();
	}

	public Value getFactoryComponent() {
		return factoryComponent;
	}

	public MethodInjectionMetadata getFactoryMethodMetadata() {
		return factoryMethodMetadata;
	}

	public String getInitMethodName() {
		return beanDefinition.getInitMethodName();
	}

	public Collection<PropertyInjectionMetadata> getPropertyInjectionMetadata() {
		return propertyMetadata;
	}

	public String getScope() {
		return beanDefinition.getScope();
	}

	public boolean isLazy() {
		return beanDefinition.isLazyInit();
	}
}