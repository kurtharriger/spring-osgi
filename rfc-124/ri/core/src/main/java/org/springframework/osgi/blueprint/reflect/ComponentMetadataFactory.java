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

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Internal class used for adapting Spring's bean definition to OSGi Blueprint
 * metadata. Used by {@link MetadataFactory} which acts as a facade.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
class ComponentMetadataFactory implements MetadataConstants {

	/**
	 * Builds a component metadata from the given bean definition.
	 * 
	 * @param name bean name
	 * @param beanDefinition
	 * @return
	 */
	static ComponentMetadata buildMetadata(String name, BeanDefinition beanDefinition) {
		// shortcut (to avoid re-rewrapping)
		Object metadata = beanDefinition.getAttribute(COMPONENT_METADATA_ATTRIBUTE);
		if (metadata instanceof ComponentMetadata)
			return (ComponentMetadata) metadata;

		// if no name has been given, look for one
		if (name == null) {
			name = (String) beanDefinition.getAttribute(COMPONENT_NAME);
		}

		if (isServiceExporter(beanDefinition)) {
			return new SpringServiceExportComponentMetadata(name, beanDefinition);
		}

		if (isSingleServiceImporter(beanDefinition)) {
			return new SpringUnaryServiceReferenceComponentMetadata(name, beanDefinition);
		}
		if (isCollectionImporter(beanDefinition)) {
			return new SpringCollectionBasedServiceReferenceComponentMetadata(name, beanDefinition);
		}

		return new SpringLocalComponentMetadata(name, beanDefinition);
	}

	private static boolean isServiceExporter(BeanDefinition beanDefinition) {
		return checkBeanDefinitionClassCompatibility(beanDefinition, EXPORTER_CLASS);
	}

	private static boolean isSingleServiceImporter(BeanDefinition beanDefinition) {
		return checkBeanDefinitionClassCompatibility(beanDefinition, SINGLE_SERVICE_IMPORTER_CLASS);
	}

	private static boolean isCollectionImporter(BeanDefinition beanDefinition) {
		return checkBeanDefinitionClassCompatibility(beanDefinition, MULTI_SERVICE_IMPORTER_CLASS);
	}

	private static boolean checkBeanDefinitionClassCompatibility(BeanDefinition definition, Class<?> clazz) {
		if (definition instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition abstractDefinition = (AbstractBeanDefinition) definition;
			if (abstractDefinition.hasBeanClass()) {
				Class<?> beanClass = abstractDefinition.getBeanClass();
				return clazz.isAssignableFrom(beanClass);
			}
		}
		return (clazz.getName().equals(definition.getBeanClassName()));
	}
}