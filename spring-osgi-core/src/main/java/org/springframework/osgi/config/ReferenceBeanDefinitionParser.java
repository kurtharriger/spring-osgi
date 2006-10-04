/*
 * Copyright 2002-2006 the original author or authors.
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
 *
 */
package org.springframework.osgi.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * @author Andy Piper
 * @since 2.1
 */
class ReferenceBeanDefinitionParser extends AbstractBeanDefinitionParser
{
	public static final String PROPERTIES = "properties";

	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(OsgiServiceProxyFactoryBean.class);
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
			} else if (ParserUtils.LISTENER_ID.equals(name)) {
				ParserUtils.parseListeners(attribute,  builder);
			} else {
				builder.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
			}
		}

		parserContext.getDelegate().parsePropertyElements(element, builder.getBeanDefinition());
		return builder.getBeanDefinition();
	}
}
