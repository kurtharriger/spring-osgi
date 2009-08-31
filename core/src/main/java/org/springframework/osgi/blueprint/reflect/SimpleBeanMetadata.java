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

import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.Target;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.osgi.blueprint.config.internal.BlueprintParser;
import org.springframework.util.StringUtils;

/**
 * Default {@link LocalComponentMetadata} implementation based on Spring's {@link BeanDefinition}.
 * 
 * @author Costin Leau
 */
class SimpleBeanMetadata extends SimpleComponentMetadata implements BeanMetadata {

	private final List<BeanArgument> arguments;
	private final List<BeanProperty> properties;

	private final String factoryMethod;
	private final Target factoryComponent;
	private final String scope;

	/**
	 * Constructs a new <code>SpringLocalComponentMetadata</code> instance.
	 * 
	 * @param name bean name
	 * @param definition Spring bean definition
	 */
	public SimpleBeanMetadata(String name, BeanDefinition definition) {
		super(name, definition);

		final String factoryMtd = definition.getFactoryMethodName();
		if (StringUtils.hasText(factoryMtd)) {
			factoryMethod = factoryMtd;
			String factory = definition.getFactoryBeanName();
			if (StringUtils.hasText(factory)) {
				factoryComponent = new SimpleRefMetadata(factory);
			} else {
				factoryComponent = null;
			}
		} else {
			factoryComponent = null;
			factoryMethod = null;
		}

		arguments = MetadataUtils.getBeanArguments(definition);
		properties = MetadataUtils.getBeanProperties(definition);

		// double check if the definition had "scope" declared
		boolean hasAttribute = definition.hasAttribute(BlueprintParser.DECLARED_SCOPE);
		scope = (hasAttribute ? (StringUtils.hasText(name) ? beanDefinition.getScope() : null) : null);
	}

	public List<BeanArgument> getArguments() {
		return arguments;
	}

	public String getClassName() {
		return beanDefinition.getBeanClassName();
	}

	public String getDestroyMethod() {
		return beanDefinition.getDestroyMethodName();
	}

	public Target getFactoryComponent() {
		return factoryComponent;
	}

	public String getFactoryMethod() {
		return factoryMethod;
	}

	public String getInitMethod() {
		return beanDefinition.getInitMethodName();
	}

	public List<BeanProperty> getProperties() {
		return properties;
	}

	public Class<?> getRuntimeClass() {
		return beanDefinition.getBeanClass();
	}

	public String getScope() {
		return scope;
	}
}