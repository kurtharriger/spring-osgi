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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.core.Conventions;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * AbstractSingleBeanDefinitionParser that supports the "depends-on" attribute.
 *
 * @author Andy Piper
 */
public abstract class DependentAbstractSingleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser
{
	public static final String LAZY_INIT = "lazy-init";

	protected void doParse(Element element, BeanDefinitionBuilder builder) {

		NamedNodeMap attributes = element.getAttributes();
		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			String name = attribute.getLocalName();

			if (ID_ATTRIBUTE.equals(name)) {
				continue;
			} else if (ParserUtils.DEPENDS_ON.equals(name)) {
				ParserUtils.parseDependsOn(attribute,  builder);
			} else if (LAZY_INIT.equals(name)) {
				builder.setLazyInit(Boolean.getBoolean(attribute.getValue()));
			} else if (!doParseAttribute(attribute, builder)) {
				builder.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
			}
		}
	}

	protected boolean doParseAttribute(Attr attribute, BeanDefinitionBuilder builder) {
		return false;
	}
}
