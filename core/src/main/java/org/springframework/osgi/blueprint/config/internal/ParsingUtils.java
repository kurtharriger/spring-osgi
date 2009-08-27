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

package org.springframework.osgi.blueprint.config.internal;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility parsing class.
 * 
 * @author Costin Leau
 */
public class ParsingUtils {

	public static final String BLUEPRINT_GENERATED_NAME_PREFIX = ".";
	/** Constant for the id attribute */
	public static final String ID_ATTRIBUTE = "id";
	/** Reserved blueprint constants */
	private static final String[] RESERVED_NAMES =
			new String[] { "blueprintContainer", "blueprintBundle", "blueprintBundleContext", "blueprintConverter" };
	public static final String BLUEPRINT_MARKER_NAME = "org.springframework.osgi.blueprint.config.internal.marker";

	public static BeanDefinitionHolder decorateAndRegister(Element ele, BeanDefinitionHolder bdHolder,
			ParserContext parserContext) {
		if (bdHolder != null) {
			bdHolder = decorateBeanDefinitionIfRequired(ele, bdHolder, parserContext);
		}

		return register(ele, bdHolder, parserContext);
	}

	public static BeanDefinitionHolder register(Element ele, BeanDefinitionHolder bdHolder, ParserContext parserContext) {
		if (bdHolder != null) {
			String name = bdHolder.getBeanName();
			checkReservedName(name, ele, parserContext);
			checkUniqueName(name, parserContext.getRegistry());
			try {
				// add non-lenient constructor resolution
				BeanDefinition beanDefinition = bdHolder.getBeanDefinition();
				if (beanDefinition instanceof AbstractBeanDefinition) {
					AbstractBeanDefinition abd = (AbstractBeanDefinition) beanDefinition;
					abd.setLenientConstructorResolution(false);
					abd.setNonPublicAccessAllowed(false);
				}

				// Register the final decorated instance.
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, parserContext.getRegistry());
			} catch (BeanDefinitionStoreException ex) {
				parserContext.getReaderContext().error(
						"Failed to register bean definition with name '" + bdHolder.getBeanName() + "'", ele, ex);
			}
			// register component (and send registration events)
			parserContext.registerComponent(new BeanComponentDefinition(bdHolder));
		}
		return bdHolder;
	}

	private static void checkUniqueName(String beanName, BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(beanName)) {
			throw new BeanDefinitionStoreException(beanName, "Duplicate definitions named [" + beanName + "] detected.");
		}
	}

	public static BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele,
			BeanDefinitionHolder originalDefinition, ParserContext parserContext) {

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

	public static BeanDefinitionHolder decorateIfRequired(Node node, BeanDefinitionHolder originalDef,
			ParserContext parserContext) {

		String namespaceUri = node.getNamespaceURI();
		if (!parserContext.getDelegate().isDefaultNamespace(namespaceUri) && !isRFC124Namespace(namespaceUri)) {
			NamespaceHandler handler =
					parserContext.getReaderContext().getNamespaceHandlerResolver().resolve(namespaceUri);
			if (handler != null) {
				return handler.decorate(node, originalDef, new ParserContext(parserContext.getReaderContext(),
						parserContext.getDelegate()));
			} else if (namespaceUri.startsWith("http://www.springframework.org/")) {
				parserContext.getReaderContext().error(
						"Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]",
						node);
			} else {
				// A custom namespace, not to be handled by Spring - maybe "xml:...".
			}
		}
		return originalDef;
	}

	public static boolean isRFC124Namespace(Node node) {
		return (BlueprintParser.NAMESPACE_URI.equals(node.getNamespaceURI()));
	}

	public static boolean isRFC124Namespace(String namespaceURI) {
		return (BlueprintParser.NAMESPACE_URI.equals(namespaceURI));
	}

	/**
	 * Generates a Blueprint specific bean name.
	 * 
	 * @param definition
	 * @param registry
	 * @param isInnerBean
	 * @return
	 * @throws BeanDefinitionStoreException
	 */
	public static String generateBlueprintBeanName(BeanDefinition definition, BeanDefinitionRegistry registry,
			boolean isInnerBean) throws BeanDefinitionStoreException {

		String initialName =
				BLUEPRINT_GENERATED_NAME_PREFIX
						+ BeanDefinitionReaderUtils.generateBeanName(definition, registry, isInnerBean);

		String generatedName = initialName;
		int counter = 0;
		while (registry.containsBeanDefinition(generatedName)) {
			generatedName = initialName + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR + counter;
			counter++;
		}

		return generatedName;
	}

	public static String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext,
			boolean shouldGenerateId, boolean shouldGenerateIdAsFallback) throws BeanDefinitionStoreException {
		if (shouldGenerateId) {
			return generateBlueprintBeanName(definition, parserContext.getRegistry(), false);
		} else {
			String id = element.getAttribute(ID_ATTRIBUTE);
			if (!StringUtils.hasText(id) && shouldGenerateIdAsFallback) {
				id = generateBlueprintBeanName(definition, parserContext.getRegistry(), false);
			}
			return id;
		}
	}

	public static boolean isReservedName(String name, Element element, ParserContext parserContext) {
		for (String reservedName : RESERVED_NAMES) {
			if (reservedName.equals(name)) {
				return true;
			}
		}
		return false;
	}

	public static void checkReservedName(String name, Element element, ParserContext parserContext) {
		if (isReservedName(name, element, parserContext)) {
			parserContext.getReaderContext().error("Blueprint reserved name '" + name + "' cannot be used", element,
					null, null);
		}
	}
}