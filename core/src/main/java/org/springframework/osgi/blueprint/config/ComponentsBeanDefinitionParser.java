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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.blueprint.config.internal.BlueprintCollectionBeanDefinitionParser;
import org.springframework.osgi.blueprint.config.internal.BlueprintReferenceBeanDefinitionParser;
import org.springframework.osgi.blueprint.config.internal.BlueprintServiceDefinitionParser;
import org.springframework.osgi.blueprint.config.internal.ComponentParser;
import org.springframework.osgi.blueprint.config.internal.ParsingUtils;
import org.springframework.osgi.service.importer.support.CollectionType;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Namespace parser handling the root &lt;components&gt; element from RFC124 (the equivalent of Spring's &lt;beans&gt;
 * element).
 * 
 * @author Costin Leau
 */
class ComponentsBeanDefinitionParser implements BeanDefinitionParser {

	static final String BLUEPRINT = "blueprint";

	private static final String DESCRIPTION = "description";
	private static final String BEAN = "bean";
	static final String REFERENCE = "reference";
	static final String SERVICE = "service";
	static final String REF_LIST = "ref-list";
	static final String REF_SET = "ref-set";

	public BeanDefinition parse(Element componentsRootElement, ParserContext parserContext) {
		// re-initialize defaults
		BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
		delegate.initDefaults(componentsRootElement);

		NodeList nl = componentsRootElement.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element ele = (Element) node;
				String namespaceUri = ele.getNamespaceURI();
				// check beans namespace
				if (delegate.isDefaultNamespace(namespaceUri)) {
					BeanDefinitionHolder holder = delegate.parseBeanDefinitionElement(ele);
					ParsingUtils.decorateAndRegister(ele, holder, parserContext);
				}
				// handle own components
				else if (ComponentParser.NAMESPACE_URI.equals(namespaceUri)) {
					parseTopLevelElement(ele, parserContext);
				}
				// leave the delegate to find a parser for it
				else {
					delegate.parseCustomElement(ele);
				}
			}
		}

		return null;
	}

	/**
	 * Parses the top elements belonging to the RFC 124 namespace. Namely these are &lt;component&gt;,
	 * &lt;description&gt; and &lt;type-converters&gt;
	 * 
	 * @param ele
	 * @param parserContext
	 */
	protected void parseTopLevelElement(Element ele, ParserContext parserContext) {
		// description
		if (DomUtils.nodeNameEquals(ele, DESCRIPTION)) {
			// ignore description for now
		} else if (DomUtils.nodeNameEquals(ele, BEAN)) {
			parseComponentElement(ele, parserContext);
		} else if (DomUtils.nodeNameEquals(ele, REFERENCE)) {
			parseReferenceElement(ele, parserContext);
		} else if (DomUtils.nodeNameEquals(ele, SERVICE)) {
			parseServiceElement(ele, parserContext);
		} else if (DomUtils.nodeNameEquals(ele, REF_LIST)) {
			parseListElement(ele, parserContext);
		} else if (DomUtils.nodeNameEquals(ele, REF_SET)) {
			parseSetElement(ele, parserContext);
		} else if (DomUtils.nodeNameEquals(ele, TypeConverterBeanDefinitionParser.TYPE_CONVERTERS)) {
			parseConvertersElement(ele, parserContext);
		} else {
			throw new IllegalArgumentException("Unknown element " + ele);
		}
	}

	/**
	 * Parses a &lt;component&gt element.
	 * 
	 * @param ele
	 * @param parserContext
	 */
	protected void parseComponentElement(Element ele, ParserContext parserContext) {
		BeanDefinitionHolder holder = new ComponentParser().parseAsHolder(ele, parserContext);
		ParsingUtils.decorateAndRegister(ele, holder, parserContext);
	}

	/**
	 * Parses a &lt;type-converters&gt;.
	 * 
	 * @param ele
	 * @param parserContext
	 */
	protected void parseConvertersElement(Element ele, ParserContext parserContext) {
		BeanDefinitionParser parser = new TypeConverterBeanDefinitionParser();
		parser.parse(ele, parserContext);
	}

	private void parseReferenceElement(Element ele, ParserContext parserContext) {
		BeanDefinitionParser parser = new BlueprintReferenceBeanDefinitionParser();
		parser.parse(ele, parserContext);
	}

	private void parseServiceElement(Element ele, ParserContext parserContext) {
		BeanDefinitionParser parser = new BlueprintServiceDefinitionParser();
		parser.parse(ele, parserContext);
	}

	private void parseListElement(Element ele, ParserContext parserContext) {
		BeanDefinitionParser parser = new BlueprintCollectionBeanDefinitionParser() {

			@Override
			protected CollectionType collectionType() {
				return CollectionType.LIST;
			}
		};
		parser.parse(ele, parserContext);
	}

	private void parseSetElement(Element ele, ParserContext parserContext) {
		BeanDefinitionParser parser = new BlueprintCollectionBeanDefinitionParser() {

			@Override
			protected CollectionType collectionType() {
				return CollectionType.SET;
			}
		};

		parser.parse(ele, parserContext);
	}
}