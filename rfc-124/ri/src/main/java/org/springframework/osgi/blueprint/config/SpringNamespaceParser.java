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

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Namespace parser handling the translation of bean related elements into from
 * RFC 124 schema into Spring/beans one.
 * 
 * @author Costin Leau
 */
class SpringNamespaceParser implements BeanDefinitionParser {

	static final String COMPONENTS = "components";

	private static final String DESCRIPTION = "description";

	private static final String TYPE_CONVERTERS = "type-converters";

	private static final String COMPONENT = "component";

	private static final String FACTORY_COMPONENT = "factory-component";


	public BeanDefinition parse(Element componentsElement, ParserContext parserContext) {
		// re-initialize defaults
		parserContext.getDelegate().initDefaults(componentsElement);
		parseComponents(componentsElement, parserContext);
		parseTypeConverters(componentsElement, parserContext);
		// this parser cannot be nested

		return null;
	}

	private void parseComponents(Element componentsElement, ParserContext parserContext) {
		List componentList = DomUtils.getChildElementsByTagName(componentsElement, COMPONENT);
		for (Iterator iterator = componentList.iterator(); iterator.hasNext();) {
			Element component = (Element) iterator.next();
			processBeanDefinition(translateComponentToBean(component), parserContext);
		}
	}

	private Element translateComponentToBean(Element component) {
		Document doc = component.getOwnerDocument();
		// create bean element
		Element beanElement = doc.createElementNS(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI,
			BeanDefinitionParserDelegate.BEAN_ELEMENT);

		// copy attributes and children
		NamedNodeMap attrs = component.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr attribute = (Attr) attrs.item(i);
			Attr transformedAttribute = null;
			// handle factory-component
			if (FACTORY_COMPONENT.equals(attribute.getLocalName())) {
				transformedAttribute = doc.createAttributeNS(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI,
					BeanDefinitionParserDelegate.FACTORY_BEAN_ATTRIBUTE);
				transformedAttribute.setNodeValue(attribute.getNodeValue());
			}
			// no need for transformation, just copy the attribute
			else {
				transformedAttribute = (Attr) attribute.cloneNode(false);
			}
			beanElement.setAttributeNodeNS(transformedAttribute);
		}

		return beanElement;
	}

	private void parseTypeConverters(Element componentsElement, ParserContext parserContext) {
		Element converters = DomUtils.getChildElementByTagName(componentsElement, TYPE_CONVERTERS);
		System.out.println("NIY: Need to parse converters " + converters);
	}

	private static void processBeanDefinition(Element ele, ParserContext parserContext) {
		BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
		XmlReaderContext readerContext = parserContext.getReaderContext();

		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// Register the final decorated instance.
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, readerContext.getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				readerContext.error("Failed to register bean definition with name '" + bdHolder.getBeanName() + "'",
					ele, ex);
			}
			// Send registration event.
			readerContext.fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}
}
