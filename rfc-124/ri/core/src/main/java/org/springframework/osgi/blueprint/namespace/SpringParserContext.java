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

package org.springframework.osgi.blueprint.namespace;

import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.ParserContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.osgi.blueprint.reflect.MetadataFactory;
import org.w3c.dom.Node;

/**
 * Spring based {@link ParserContext} implementation.
 * 
 * @author Costin Leau
 * 
 */
public class SpringParserContext implements ParserContext {

	private static final String COMPONENT_NAME_ATTR = "spring.osgi.component.name";
	private final org.springframework.beans.factory.xml.ParserContext parserContext;
	private final ComponentDefinitionRegistry registry;


	public SpringParserContext(org.springframework.beans.factory.xml.ParserContext parserContext) {
		this.parserContext = parserContext;
		this.registry = new SpringComponentDefinitionRegistry(parserContext.getRegistry());
	}

	public ComponentDefinitionRegistry getComponentDefinitionRegistry() {
		return registry;
	}

	public ComponentMetadata getEnclosingComponent() {
		BeanDefinition def = parserContext.getContainingBeanDefinition();
		if (def != null) {
			String beanName = null;
			// check the short firsts
			if (!def.hasAttribute(COMPONENT_NAME_ATTR)) {
				BeanDefinitionRegistry defRegistry = parserContext.getRegistry();
				String[] names = defRegistry.getBeanDefinitionNames();
				for (String name : names) {
					// TODO: is the equality check enough?
					if (def == defRegistry.getBeanDefinition(name)) {
						beanName = name;
					}
				}
			}

			return MetadataFactory.buildComponentMetadataFor(beanName, def);
		}
		else {
			return null;
		}
	}

	public Node getSourceNode() {
		throw new UnsupportedOperationException("not implemented yet");
	}
}