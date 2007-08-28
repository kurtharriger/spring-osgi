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

import java.util.Set;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.config.ParserUtils.AttributeCallback;
import org.springframework.osgi.service.exporter.OsgiServiceFactoryBean;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * BeanDefinitionParser for service element found in the osgi namespace.
 * 
 * @author Costin Leau
 * @author Hal Hildebrand
 * @author Andy Piper
 */
class ServiceBeanDefinitionParser extends AbstractBeanDefinitionParser {
	private static final String TARGET_BEAN_NAME = "targetBeanName";

	public static final String ACTIVATION_ID = "activation-method";

	public static final String DEACTIVATION_ID = "deactivation-method";

	public static final String INTERFACES_ID = "interfaces";

	public static final String INTERFACE = "interface";

	public static final String PROPS_ID = "service-properties";

	public static final String REF = "ref";

	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(OsgiServiceFactoryBean.class);

		// parse attributes
		ParserUtils.parseCustomAttributes(element, builder, new AttributeCallback() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.springframework.osgi.config.ParserUtils.AttributeCallback#process(org.w3c.dom.Element,
			 * org.w3c.dom.Attr)
			 */
			public void process(Element parent, Attr attribute, BeanDefinitionBuilder bldr) {
				String name = attribute.getLocalName();

				if (INTERFACE.equals(name)) {
					bldr.addPropertyValue(INTERFACES_ID, attribute.getValue());
				}
				else if (REF.equals(name)) {
					;
				}
				// fallback mechanism
				else {
					bldr.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
				}
			}
		});

		// determine nested/referred beans
		Object target = null;
		if (element.hasAttribute(REF))
			target = new RuntimeBeanReference(element.getAttribute(REF));

		NodeList nl = element.getChildNodes();

		// parse all sub elements
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element subElement = (Element) node;

				// osgi:interfaces
				if (INTERFACES_ID.equals(subElement.getLocalName())) {
					// check shortcut
					if (element.hasAttribute(INTERFACE)) {
						parserContext.getReaderContext().error(
								"either 'interface' attribute or <intefaces> sub-element has be specified", element);
					}
					Set interfaces = parserContext.getDelegate().parseSetElement(subElement,
							builder.getBeanDefinition());
					builder.addPropertyValue(INTERFACES_ID, interfaces);
				}

				// osgi:service-properties
				else if (PROPS_ID.equals(subElement.getLocalName())) {
                    if (DomUtils.getChildElementsByTagName(subElement, BeanDefinitionParserDelegate.ENTRY_ELEMENT).size()>0) {
                        Object props = parserContext.getDelegate().parseMapElement(subElement, builder.getRawBeanDefinition());
					    builder.addPropertyValue(Conventions.attributeNameToPropertyName(PROPS_ID), props);
                    }
                    else if (DomUtils.getChildElementsByTagName(subElement, BeanDefinitionParserDelegate.PROP_ELEMENT).size()>0) {
                        Object props = parserContext.getDelegate().parsePropsElement(subElement);
					    builder.addPropertyValue(Conventions.attributeNameToPropertyName(PROPS_ID), props);
                    }
                    else {
                        parserContext.getReaderContext().error("Invalid service property type", subElement);
                    }
                }
				// nested bean reference/declaration
				else {
					if (element.hasAttribute(REF))
						parserContext.getReaderContext().error(
								"nested bean definition/reference cannot be used when attribute 'ref' is specified",
								element);
					target = parserContext.getDelegate().parsePropertySubElement(subElement,
							builder.getBeanDefinition());
				}
			}
		}

		// do we have a bean reference ?
		if (target instanceof RuntimeBeanReference) {
			builder.addPropertyValue(TARGET_BEAN_NAME, ((RuntimeBeanReference) target).getBeanName());
		}
		
		builder.addPropertyValue("target", target);
		return builder.getBeanDefinition();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#shouldGenerateIdAsFallback()
	 */
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

}