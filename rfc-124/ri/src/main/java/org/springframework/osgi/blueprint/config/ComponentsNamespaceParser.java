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

import java.util.Collection;
import java.util.LinkedHashSet;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ParseState;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Namespace parser handling the root &lt;components&gt; element from RFC124
 * (the equivalent of Spring's &lt;beans&gt; element).
 * 
 * @author Costin Leau
 */
class ComponentsNamespaceParser implements BeanDefinitionParser {

	static final String COMPONENTS = "components";
	// RFC 124 namespace
	public static final String NAMESPACE_URI = "http://www.osgi.org/xmlns/blueprint/v1.0.0";

	private static final String DESCRIPTION = "description";

	private static final String TYPE_CONVERTERS = "type-converters";

	private Collection usedNames = new LinkedHashSet();

	private ParseState parseState = new ParseState();


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
					delegate.parseBeanDefinitionElement(ele);
				}
				// handle own components
				else if (NAMESPACE_URI.equals(namespaceUri)) {
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
	 * Parses the top elements belonging to the RFC 124 namespace. Namely these
	 * are &lt;component&gt;, &lt;description&gt; and &lt;type-converters&gt;
	 * 
	 * @param ele
	 * @param parserContext
	 */
	protected void parseTopLevelElement(Element ele, ParserContext parserContext) {
		// description
		if (DomUtils.nodeNameEquals(ele, DESCRIPTION)) {
			// ignore description for now
		}
		else if (DomUtils.nodeNameEquals(ele, ComponentParser.COMPONENT)) {
			parseComponentElement(ele, parserContext);
		}
		else if (DomUtils.nodeNameEquals(ele, TYPE_CONVERTERS)) {
			parseConvertersElement(ele, parserContext);
		}
		else {
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
		new ComponentParser(parseState, usedNames).parse(ele, parserContext);
	}

	/**
	 * Parses a &lt;type-converters&gt;.
	 * 
	 * @param ele
	 * @param parserContext
	 */
	protected void parseConvertersElement(Element ele, ParserContext parserContext) {
		throw new UnsupportedOperationException(TYPE_CONVERTERS + " element not supported yet");
	}
}
