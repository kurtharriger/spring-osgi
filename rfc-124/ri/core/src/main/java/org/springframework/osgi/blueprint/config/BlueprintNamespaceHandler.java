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

package org.springframework.osgi.blueprint.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.osgi.blueprint.config.internal.BlueprintCollectionBeanDefinitionParser;
import org.springframework.osgi.blueprint.config.internal.BlueprintReferenceBeanDefinitionParser;
import org.springframework.osgi.blueprint.config.internal.BlueprintServiceDefinitionParser;
import org.springframework.osgi.blueprint.config.internal.ComponentParser;
import org.springframework.osgi.service.importer.support.CollectionType;

/**
 * Spring-based namespace handler for the blueprint/RFC-124 core namespace.
 * 
 * @author Costin Leau
 * 
 */
class BlueprintNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		registerBeanDefinitionParser(ComponentsBeanDefinitionParser.BLUEPRINT, new ComponentsBeanDefinitionParser());
		registerBeanDefinitionParser(ComponentParser.BEAN, new ComponentBeanDefinitionParser());
		registerBeanDefinitionParser(TypeConverterBeanDefinitionParser.TYPE_CONVERTERS,
				new TypeConverterBeanDefinitionParser());

		// Spring DM constructs
		registerBeanDefinitionParser(ComponentsBeanDefinitionParser.REFERENCE,
				new BlueprintReferenceBeanDefinitionParser());

		registerBeanDefinitionParser(ComponentsBeanDefinitionParser.REF_LIST,
				new BlueprintCollectionBeanDefinitionParser() {

					protected CollectionType collectionType() {
						return CollectionType.LIST;
					}
				});
		registerBeanDefinitionParser(ComponentsBeanDefinitionParser.REF_SET,
				new BlueprintCollectionBeanDefinitionParser() {

					protected CollectionType collectionType() {
						return CollectionType.SET;
					}
				});

		registerBeanDefinitionParser(ComponentsBeanDefinitionParser.SERVICE, new BlueprintServiceDefinitionParser());
	}
}