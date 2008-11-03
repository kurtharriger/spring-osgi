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

package org.springframework.osgi.blueprint.config;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility parsing class.
 * 
 * @author Costin Leau
 */
class ParsingUtils {

	static BeanDefinitionHolder decorateAndRegister(Element ele, BeanDefinitionHolder bdHolder,
			ParserContext parserContext) {
		if (bdHolder != null) {
			bdHolder = decorateBeanDefinitionIfRequired(ele, bdHolder, parserContext);
		}

		return register(ele, bdHolder, parserContext);
	}

	static BeanDefinitionHolder register(Element ele, BeanDefinitionHolder bdHolder, ParserContext parserContext) {
		if (bdHolder != null) {
			try {
				// Register the final decorated instance.
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder,
					parserContext.getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				parserContext.getReaderContext().error(
					"Failed to register bean definition with name '" + bdHolder.getBeanName() + "'", ele, ex);
			}
			// register component (and send registration events)
			parserContext.registerComponent(new BeanComponentDefinition(bdHolder));
		}
		return bdHolder;
	}

	static BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder originalDefinition,
			ParserContext parserContext) {

		BeanDefinitionHolder finalDefinition = originalDefinition;

		// Decorate based on custom attributes first.
		NamedNodeMap attributes = ele.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node node = attributes.item(i);
			finalDefinition = decorateIfRequired(node, finalDefinition, parserContext);
		}

		// Decorate based on custom nested elements.
		NodeList children = ele.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				finalDefinition = decorateIfRequired(node, finalDefinition, parserContext);
			}
		}
		return finalDefinition;
	}

	static BeanDefinitionHolder decorateIfRequired(Node node, BeanDefinitionHolder originalDef,
			ParserContext parserContext) {

		String namespaceUri = node.getNamespaceURI();
		if (!parserContext.getDelegate().isDefaultNamespace(namespaceUri) && !isRFC124Namespace(namespaceUri)) {
			NamespaceHandler handler = parserContext.getReaderContext().getNamespaceHandlerResolver().resolve(
				namespaceUri);
			if (handler != null) {
				return handler.decorate(node, originalDef, new ParserContext(parserContext.getReaderContext(),
					parserContext.getDelegate()));
			}
			else if (namespaceUri.startsWith("http://www.springframework.org/")) {
				parserContext.getReaderContext().error(
					"Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", node);
			}
			else {
				// A custom namespace, not to be handled by Spring - maybe "xml:...".
			}
		}
		return originalDef;
	}

	static boolean isRFC124Namespace(Node node) {
		return (ComponentsBeanDefinitionParser.NAMESPACE_URI.equals(node.getNamespaceURI()));
	}

	static boolean isRFC124Namespace(String namespaceURI) {
		return (ComponentsBeanDefinitionParser.NAMESPACE_URI.equals(namespaceURI));
	}
}
