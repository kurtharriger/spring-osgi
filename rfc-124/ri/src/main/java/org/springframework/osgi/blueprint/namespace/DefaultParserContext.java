/*
 * Copyright 2008 the original author or authors.
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
import org.w3c.dom.Node;

public class DefaultParserContext implements ParserContext {
	
	private final ComponentDefinitionRegistry registry;
	private final ComponentMetadata enclosingComponent;
	private final Node sourceNode;
	
	public DefaultParserContext(ComponentDefinitionRegistry reg,
			ComponentMetadata component,
			Node node) {
		this.registry = reg;
		this.enclosingComponent = component;
		this.sourceNode = node;
	}

	public ComponentDefinitionRegistry getComponentDefinitionRegistry() {
		return this.registry;
	}

	public ComponentMetadata getEnclosingComponent() {
		return this.enclosingComponent;
	}

	public Node getSourceNode() {
		return this.sourceNode;
	}

}
