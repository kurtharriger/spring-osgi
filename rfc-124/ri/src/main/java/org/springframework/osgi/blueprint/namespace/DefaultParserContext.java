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
