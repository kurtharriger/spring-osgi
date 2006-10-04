/*
 * Copyright 2002-2005 the original author or authors.
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
package org.springframework.osgi.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.service.OsgiServiceExporter;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.util.Properties;

/**
 * @author Hal Hildebrand
 * @author Andy Piper
 */
public class ServiceBeanDefinitionParser extends AbstractBeanDefinitionParser
{
	public final static String ACTIVATION_ID = "activation-method";
	public final static String DEACTIVATION_ID = "deactivation-method";
	public final static String INTERFACES_ID = "interfaces";
	public final static String PROPS_ID = "service-properties";


    protected boolean autogenerateId() {
        return true;
    }

	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(OsgiServiceExporter.class);
		NamedNodeMap attributes = element.getAttributes();
		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			String name = attribute.getLocalName();

			if (ID_ATTRIBUTE.equals(name)) {
				continue;
			} else if (ParserUtils.DEPENDS_ON.equals(name)) {
				ParserUtils.parseDependsOn(attribute,  builder);
			} else if (ParserUtils.LAZY_INIT.equals(name)) {
				builder.setLazyInit(Boolean.getBoolean(attribute.getValue()));
			} else if ("interface".equals(name)) {
				builder.addPropertyValue(INTERFACES_ID,	attribute.getValue());
			} else {
				builder.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
			}
		}

	  Element e = DomUtils.getChildElementByTagName(element, INTERFACES_ID);
		if (e != null) {
			NodeList nl = e.getElementsByTagName("*");
			for (int i = 0; i<nl.getLength(); i++) {
				if (nl.item(i) instanceof Element) {
					Object interfaces = parserContext.getDelegate().parsePropertySubElement
							((Element)nl.item(i), null, String.class.getName());
					builder.addPropertyValue(INTERFACES_ID, interfaces);
				}
			}
		}

		e = DomUtils.getChildElementByTagName(element, PROPS_ID);
		if (e != null) {
			Properties props = parserContext.getDelegate().parsePropsElement(e);
			builder.addPropertyValue(Conventions.attributeNameToPropertyName(PROPS_ID), props);
		}
		return builder.getBeanDefinition();
	}
}
