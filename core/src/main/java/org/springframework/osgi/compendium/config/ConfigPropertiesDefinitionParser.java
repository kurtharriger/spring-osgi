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

package org.springframework.osgi.compendium.config;

import java.util.Properties;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.compendium.cm.ConfigAdminPropertiesFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Simple namespace parser for osgix:config-properties. Extends Single bean
 * definition parser (instead of the simpleBeanDefParser) to properly filter
 * attributes based on the declared namespace.
 * 
 * @author Costin Leau
 */
public class ConfigPropertiesDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String PROPERTIES_PROP = "properties";


	protected Class getBeanClass(Element element) {
		return ConfigAdminPropertiesFactoryBean.class;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		NamedNodeMap attributes = element.getAttributes();
		String nsURI = element.getNamespaceURI();
		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			String attrURI = attribute.getNamespaceURI();

			// check namespace equality
			if (attrURI == null || attrURI.equals(nsURI)) {
				if (!ID_ATTRIBUTE.equals(attribute.getLocalName())) {
					String propertyName = Conventions.attributeNameToPropertyName(attribute.getLocalName());
					Assert.state(StringUtils.hasText(propertyName), "Illegal property name discovered");
					builder.addPropertyValue(propertyName, attribute.getValue());
				}
			}
			// try to find the custom namespace
			else {
				BeanDefinitionHolder bdh = new BeanDefinitionHolder(builder.getRawBeanDefinition(), "<ignored>");

				//				parserContext.getDelegate().decorateBeanDefinitionIfRequired(element, bdh);
				//				parserContext.getDelegate().parseBeanDefinitionElement(element, builder.getRawBeanDefinition());

				NamespaceHandler handler = parserContext.getReaderContext().getNamespaceHandlerResolver().resolve(
					attrURI);
				if (handler != null) {
					handler.decorate(attribute, bdh, parserContext);
				}
				else {
					parserContext.getReaderContext().warning(
						"No Spring NamespaceHandler found for XML schema namespace [" + attrURI + "]", attribute);
				}
			}
		}

		Properties parsedProps = parserContext.getDelegate().parsePropsElement(element);
		if (!parsedProps.isEmpty()) {
			if (builder.getRawBeanDefinition().getPropertyValues().contains(PROPERTIES_PROP)) {
				parserContext.getReaderContext().error(
					"Property '" + PROPERTIES_PROP
							+ "' is defined more then once. Only one approach may be used per property.", element);

			}
			builder.addPropertyValue(PROPERTIES_PROP, parsedProps);
		}
	}
}
