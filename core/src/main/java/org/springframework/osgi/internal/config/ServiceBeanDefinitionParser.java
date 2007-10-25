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
package org.springframework.osgi.internal.config;

import java.util.Set;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.internal.config.ParserUtils.AttributeCallback;
import org.springframework.osgi.internal.service.exporter.OsgiServiceRegistrationListenerWrapper;
import org.springframework.osgi.service.exporter.OsgiServiceFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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

	// bean properties
	private static final String TARGET_BEAN_NAME_PROP = "targetBeanName";

	private static final String TARGET_PROP = "target";

	private static final String LISTENERS_PROP = "listeners";

	private static final String INTERFACES_PROP = "interfaces";

	// XML elements
	private static final String INTERFACES_ID = "interfaces";

	private static final String INTERFACE = "interface";

	private static final String PROPS_ID = "service-properties";

	private static final String LISTENER = "registration-listener";

	private static final String REF = "ref";

	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(OsgiServiceFactoryBean.class);

		// parse attributes
		ParserUtils.parseCustomAttributes(element, builder, new AttributeCallback() {

			public void process(Element parent, Attr attribute, BeanDefinitionBuilder bldr) {
				String name = attribute.getLocalName();

				if (INTERFACE.equals(name)) {
					bldr.addPropertyValue(INTERFACES_PROP, attribute.getValue());
				}
				else if (REF.equals(name)) {
					;
				}
				// fall-back mechanism
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

		ManagedList listeners = new ManagedList();

		// parse all sub elements
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element subElement = (Element) node;

				String name = subElement.getLocalName();

				// osgi:interfaces
				if (INTERFACES_ID.equals(name)) {
					// check shortcut
					if (element.hasAttribute(INTERFACE)) {
						parserContext.getReaderContext().error(
							"either 'interface' attribute or <intefaces> sub-element has be specified", element);
					}
					Set interfaces = parserContext.getDelegate().parseSetElement(subElement,
						builder.getBeanDefinition());
					builder.addPropertyValue(INTERFACES_PROP, interfaces);
				}

				// osgi:service-properties
				else if (PROPS_ID.equals(name)) {
					if (DomUtils.getChildElementsByTagName(subElement, BeanDefinitionParserDelegate.ENTRY_ELEMENT).size() > 0) {
						Object props = parserContext.getDelegate().parseMapElement(subElement,
							builder.getRawBeanDefinition());
						builder.addPropertyValue(Conventions.attributeNameToPropertyName(PROPS_ID), props);
					}
					else {
						parserContext.getReaderContext().error("Invalid service property type", subElement);
					}
				}

				// osgi:registration-listener
				else if (LISTENER.equals(name)) {
					listeners.add(getListener(parserContext, subElement, builder));
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
			builder.addPropertyValue(TARGET_BEAN_NAME_PROP, ((RuntimeBeanReference) target).getBeanName());
		}

		builder.addPropertyValue(TARGET_PROP, target);

		// add listeners
		builder.addPropertyValue(LISTENERS_PROP, listeners);

		return builder.getBeanDefinition();
	}

	private BeanDefinition getListener(ParserContext context, Element element, BeanDefinitionBuilder builder) {

		// filter elements
		NodeList nl = element.getChildNodes();

		// wrapped object
		Object target = null;

		// discover if we have listener with ref and nested bean declaration
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				Element beanDef = (Element) node;

				if (element.hasAttribute(REF))
					context.getReaderContext().error(
						"nested bean declaration is not allowed if 'ref' attribute has been specified", beanDef);

				target = context.getDelegate().parsePropertySubElement(beanDef, builder.getBeanDefinition());
			}
		}

		// extract registration/unregistration attributes from
		// <osgi:registration-listener>
		MutablePropertyValues vals = new MutablePropertyValues();

		NamedNodeMap attrs = element.getAttributes();
		for (int x = 0; x < attrs.getLength(); x++) {
			Attr attribute = (Attr) attrs.item(x);
			String name = attribute.getLocalName();

			if (REF.equals(name))
				target = new RuntimeBeanReference(StringUtils.trimWhitespace(attribute.getValue()));
			else
				vals.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
		}

		// create serviceListener wrapper
		RootBeanDefinition wrapperDef = new RootBeanDefinition(OsgiServiceRegistrationListenerWrapper.class);

		ConstructorArgumentValues cav = new ConstructorArgumentValues();
		cav.addIndexedArgumentValue(0, target);

		wrapperDef.setConstructorArgumentValues(cav);
		wrapperDef.setPropertyValues(vals);

		return wrapperDef;

	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}
}