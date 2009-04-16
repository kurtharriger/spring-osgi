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

package org.springframework.osgi.blueprint.compendium.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.osgi.compendium.config.internal.ConfigPropertiesDefinitionParser;
import org.springframework.osgi.compendium.config.internal.ManagedPropertiesDefinitionParser;
import org.springframework.osgi.compendium.config.internal.ManagedServiceFactoryDefinitionParser;

/**
 * Spring-based namespace handler for the blueprint/RFC-124 compendium/osgix
 * namespace.
 * 
 * @author Costin Leau
 */
public class BlueprintCompendiumNamespaceHandler extends NamespaceHandlerSupport {

	static final String MANAGED_PROPS = "managed-properties";
	static final String MANAGED_FACTORY_PROPS = "managed-service-factory";
	static final String CM_PROPS = "cm-properties";


	public void init() {
		registerBeanDefinitionParser("cm-properties", new ConfigPropertiesDefinitionParser());
		registerBeanDefinitionParser("managed-service-factory", new ManagedServiceFactoryDefinitionParser());
		registerBeanDefinitionDecorator("managed-properties", new ManagedPropertiesDefinitionParser());
	}
}
