/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.core.enums.StaticLabeledEnumResolver;
import org.springframework.osgi.config.ParserUtils.AttributeCallback;
import org.springframework.osgi.service.importer.support.Cardinality;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base class for parsing reference declarations. Contains common functionality
 * such as adding listeners (and their custom methods), interfaces, cardinality
 * and so on.
 * 
 * 
 * @author Costin Leau
 * 
 */
abstract class AbstractReferenceDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * Attribute callback dealing with 'cardinality' attribute.
	 * 
	 * @author Costin Leau
	 */
	class ReferenceAttributesCallback implements AttributeCallback {
		/** global cardinality setting */
		public boolean isCardinalitySpecified = false;

		public boolean process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
			String name = attribute.getLocalName();
			String value = attribute.getValue();

			// make sure the attribute is
			if (CARDINALITY.equals(name)) {
				isCardinalitySpecified = true;
				builder.addPropertyValue(CARDINALITY_PROP, determineCardinality(value));
				return false;
			}

			else if (SERVICE_BEAN_NAME.equals(name)) {
				builder.addPropertyValue(SERVICE_BEAN_NAME_PROP, value);
				return false;
			}

			else if (INTERFACE.equals(name)) {
				builder.addPropertyValue(INTERFACES_PROP, value);
				return false;
			}

			else if (CONTEXT_CLASSLOADER.equals(name)) {
				// convert constant to upper case to let Spring do the
				// conversion
				String val = value.toUpperCase().replace('-', '_');
				builder.addPropertyValue(CCL_PROP, val);
				return false;
			}

			return true;
		}
	};

	// Class properties
	private static final String LISTENERS_PROP = "listeners";

	private static final String CARDINALITY_PROP = "cardinality";

	private static final String SERVICE_BEAN_NAME_PROP = "serviceBeanName";

	private static final String INTERFACES_PROP = "interfaces";

	private static final String CCL_PROP = "contextClassLoader";

	// XML attributes/elements
	private static final String LISTENER = "listener";

	private static final String BIND_METHOD = "bind-method";

	private static final String UNBIND_METHOD = "unbind-method";

	private static final String REF = "ref";

	private static final String INTERFACE = "interface";

	private static final String INTERFACES = "interfaces";

	private static final String INTERFACEs = "interface";

	private static final String CARDINALITY = "cardinality";

	private static final String ZERO = "0";

	private static final String SERVICE_BEAN_NAME = "bean-name";

	private static final String CONTEXT_CLASSLOADER = "context-classloader";

	// document defaults
	protected OsgiDefaultsDefinition defaults = null;

	/**
	 * Get OSGi defaults (in case they haven't been resolved).
	 * @param document
	 * @return
	 */
	private OsgiDefaultsDefinition resolveDefaults(Document document) {
		if (defaults == null) {
			defaults = ParserUtils.initOsgiDefaults(document);
		}
		return defaults;
	}

	protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		if (defaults == null)
			resolveDefaults(element.getOwnerDocument());

		ReferenceAttributesCallback callback = new ReferenceAttributesCallback();

		parseAttributes(element, builder, new AttributeCallback[] { callback });

		if (!callback.isCardinalitySpecified) {
			applyDefaultCardinality(builder, defaults);
		}

		parseNestedElements(element, context, builder);
	}

	/**
	 * Allow subclasses to add their own callbacks.
	 * 
	 * @param element
	 * @param builder
	 * @param callbacks
	 */
	protected void parseAttributes(Element element, BeanDefinitionBuilder builder, AttributeCallback[] callbacks) {
		ParserUtils.parseCustomAttributes(element, builder, callbacks);
	}

	/**
	 * Subclasses should overide this method to provide the proper mandatory
	 * cardinality option/string.
	 * 
	 * @return mandatory cardinality as a string.
	 */
	protected abstract String mandatoryCardinality();

	/**
	 * Subclasses should overide this method to provide the proper optional
	 * cardinality option/string.
	 * 
	 * @return optional cardinality as a string
	 */
	protected abstract String optionalCardinality();

	/**
	 * Utility method declared for reusability. It maintains the
	 * optional/mandatory option of the cardinality option and returns a
	 * specialized (singular/multiple) cardinality string.
	 * 
	 * @param value cardinality string
	 * @return the specialized (singular/multiple) cardinality.
	 */
	protected Object determineCardinality(String value) {
		return processCardinalityString((value.startsWith(ZERO) ? optionalCardinality() : mandatoryCardinality()));
	}

	/**
	 * Since cardinality contains numbers and the constants name cannot start
	 * with a number we have to do conversion of the name or of the string. the
	 * latter is easier and quicker.
	 * 
	 * @param value
	 * @return
	 */
	private Cardinality processCardinalityString(String value) {
		return (Cardinality) StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(Cardinality.class, value);
	}

	/**
	 * Apply default cardinality.
	 * 
	 * @param builder
	 * @param defaults
	 */
	protected void applyDefaultCardinality(BeanDefinitionBuilder builder, OsgiDefaultsDefinition defaults) {
		builder.addPropertyValue(CARDINALITY_PROP, determineCardinality(defaults.getCardinality()));
	}

	/**
	 * Parse nested elements. In case of a reference definition, this means
	 * using the listeners.
	 * 
	 * 
	 * @param element
	 * @param context
	 * @param builder
	 */
	protected void parseNestedElements(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		parseInterfaces(element, context, builder);
		parseListeners(element, context, builder);
	}

	/**
	 * Parse interfaces.
	 * 
	 * @param element
	 * @param context
	 * @param builder
	 */
	protected void parseInterfaces(Element parent, ParserContext parserContext, BeanDefinitionBuilder builder) {

		Element element = DomUtils.getChildElementByTagName(parent, INTERFACES);
		if (element != null) {
			// check shortcut on the parent
			if (parent.hasAttribute(INTERFACE)) {
				parserContext.getReaderContext().error(
					"either 'interface' attribute or <intefaces> sub-element has be specified", parent);
			}
			Set interfaces = parserContext.getDelegate().parseSetElement(element, builder.getBeanDefinition());
			builder.addPropertyValue(INTERFACES_PROP, interfaces);
		}
	}

	/**
	 * Parse listeners.
	 * 
	 * @param element
	 * @param context
	 * @param builder
	 */
	protected void parseListeners(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		List listeners = DomUtils.getChildElementsByTagName(element, LISTENER);

		ManagedList listenersRef = new ManagedList();
		// loop on listeners
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			Element listnr = (Element) iter.next();

			// wrapper target object
			Object target = null;

			// filter elements
			NodeList nl = listnr.getChildNodes();

			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element beanDef = (Element) node;

					// check inline ref
					if (listnr.hasAttribute(REF))
						context.getReaderContext().error(
							"nested bean declaration is not allowed if 'ref' attribute has been specified", beanDef);

					target = context.getDelegate().parsePropertySubElement(beanDef, builder.getBeanDefinition());
				}
			}

			// extract bind/unbind attributes from <osgi:listener>
			// Element

			MutablePropertyValues vals = new MutablePropertyValues();

			NamedNodeMap attrs = listnr.getAttributes();
			for (int x = 0; x < attrs.getLength(); x++) {
				Attr attribute = (Attr) attrs.item(x);
				String name = attribute.getLocalName();

				if (REF.equals(name))
					target = new RuntimeBeanReference(attribute.getValue());
				else
					vals.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
			}

			// create serviceListener wrapper
			RootBeanDefinition wrapperDef = new RootBeanDefinition(OsgiServiceLifecycleListenerAdapter.class);

			ConstructorArgumentValues cav = new ConstructorArgumentValues();
			cav.addIndexedArgumentValue(0, target);

			wrapperDef.setConstructorArgumentValues(cav);
			wrapperDef.setPropertyValues(vals);
			listenersRef.add(wrapperDef);

		}

		builder.addPropertyValue(LISTENERS_PROP, listenersRef);
	}
}
