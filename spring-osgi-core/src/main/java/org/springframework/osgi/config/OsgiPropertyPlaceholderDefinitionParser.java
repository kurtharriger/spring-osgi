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
 */
package org.springframework.osgi.config;

import java.util.Properties;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.config.ParserUtils.AttributeCallback;
import org.springframework.osgi.context.support.OsgiPropertyPlaceholder;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * osgi:property-placeholder parser.
 * 
 * @author Costin Leau
 * 
 */
class OsgiPropertyPlaceholderDefinitionParser extends AbstractSingleBeanDefinitionParser {

	public static final String REF = "defaults-ref";
	public static final String PROPERTIES_FIELD = "properties";
	public static final String NESTED_PROPERTIES = "default-properties";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser#getBeanClass(org.w3c.dom.Element)
	 */
	protected Class getBeanClass(Element element) {
		return OsgiPropertyPlaceholder.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#shouldGenerateId()
	 */
	protected boolean shouldGenerateId() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser#doParse(org.w3c.dom.Element,
	 *      org.springframework.beans.factory.xml.ParserContext,
	 *      org.springframework.beans.factory.support.BeanDefinitionBuilder)
	 */
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		// do standard parsing
		ParserUtils.parseCustomAttributes(element, builder, new AttributeCallback() {

			public void process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
				String name = attribute.getLocalName();
				// transform ref into bean reference
				if (REF.equals(name))
					builder.addPropertyReference(PROPERTIES_FIELD, attribute.getValue());
				else
					// fallback to defaults
					builder.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
			}
		});

		// parse subelement (default-properties)
		Element nestedElement = DomUtils.getChildElementByTagName(element, NESTED_PROPERTIES);

		if (nestedElement != null) {
			if (element.hasAttribute(REF))
				parserContext.getReaderContext().error(
						"nested properties cannot be declared if '" + REF + "' attribute is specified", element);

			builder.addPropertyValue(PROPERTIES_FIELD, parserContext.getDelegate().parsePropsElement(nestedElement));
		}
	}

}
