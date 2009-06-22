/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.config.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReferenceFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.config.internal.adapter.OsgiServiceLifecycleListenerAdapter;
import org.springframework.osgi.config.internal.util.AttributeCallback;
import org.springframework.osgi.config.internal.util.ParserUtils;
import org.springframework.osgi.config.internal.util.ReferenceParsingUtil;
import org.springframework.osgi.service.importer.support.ImportContextClassLoaderEnum;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base class for parsing reference declarations. Contains common functionality such as adding listeners (and their
 * custom methods), interfaces, cardinality and so on.
 * 
 * <p/>
 * 
 * <strong>Note:</strong> This parser also handles the cyclic injection between an importer and its listeners by
 * breaking the chain by creating an adapter instead of the listener. The adapter will then do dependency lookup for the
 * listener.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractReferenceDefinitionParser extends AbstractBeanDefinitionParser {

	/**
	 * Attribute callback dealing with 'cardinality' attribute.
	 * 
	 * @author Costin Leau
	 */
	class ReferenceAttributesCallback implements AttributeCallback {

		public boolean process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
			String name = attribute.getLocalName();
			String value = attribute.getValue().trim();

			if (CARDINALITY.equals(name)) {
				builder.addPropertyValue(AVAILABILITY_PROP, ReferenceParsingUtil
						.determineAvailabilityFromCardinality(value));
				return false;
			}

			if (AVAILABILITY.equals(name)) {
				builder.addPropertyValue(AVAILABILITY_PROP, ReferenceParsingUtil.determineAvailability(value));
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
				String val = value.toUpperCase(Locale.ENGLISH).replace('-', '_');
				builder.addPropertyValue(CCL_PROP, ImportContextClassLoaderEnum.valueOf(val));
				return false;
			}

			return true;
		}
	};

	// Class properties
	private static final String LISTENERS_PROP = "listeners";

	private static final String AVAILABILITY_PROP = "availability";

	private static final String SERVICE_BEAN_NAME_PROP = "serviceBeanName";

	private static final String INTERFACES_PROP = "interfaces";

	private static final String CCL_PROP = "importContextClassLoader";

	private static final String TARGET_BEAN_NAME_PROP = "targetBeanName";

	private static final String TARGET_PROP = "target";

	// XML attributes/elements
	private static final String LISTENER = "listener";

	private static final String REF = "ref";

	private static final String INTERFACE = "interface";

	private static final String INTERFACES = "interfaces";

	private static final String AVAILABILITY = "availability";

	private static final String CARDINALITY = "cardinality";

	private static final String SERVICE_BEAN_NAME = "bean-name";

	private static final String CONTEXT_CLASSLOADER = "context-class-loader";

	private static final String M = "m";

	public static final String GENERATED_REF = "org.springframework.osgi.config.reference.generated";

	// document defaults
	protected OsgiDefaultsDefinition defaults = null;

	/**
	 * Get OSGi defaults (in case they haven't been resolved).
	 * 
	 * @param document
	 * @return
	 */
	protected OsgiDefaultsDefinition resolveDefaults(Document document, ParserContext parserContext) {
		return new OsgiDefaultsDefinition(document, parserContext);
	}

	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();

		Class<?> beanClass = getBeanClass(element);
		Assert.notNull(beanClass);

		if (beanClass != null) {
			builder.getRawBeanDefinition().setBeanClass(beanClass);
		}

		builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
		if (parserContext.isNested()) {
			// Inner bean definition must receive same scope as containing bean.
			builder.setScope(parserContext.getContainingBeanDefinition().getScope());
		}
		if (parserContext.isDefaultLazyInit()) {
			// Default-lazy-init applies to custom bean definitions as well.
			builder.setLazyInit(true);
		}
		doParse(element, parserContext, builder);

		AbstractBeanDefinition def = builder.getBeanDefinition();

		// check whether the bean is mandatory (and if it is, make it top-level
		// bean)
		if (parserContext.isNested()) {
			String value = element.getAttribute(AbstractBeanDefinitionParser.ID_ATTRIBUTE);
			value = (StringUtils.hasText(value) ? value + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR : "");
			String generatedName = generateBeanName(value, def, parserContext);

			BeanDefinitionHolder holder = new BeanDefinitionHolder(def, generatedName);
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
			return createBeanReferenceDefinition(generatedName, def);
		}

		return def;
	}

	private AbstractBeanDefinition createBeanReferenceDefinition(String beanName, BeanDefinition actualDef) {
		GenericBeanDefinition def = new GenericBeanDefinition();
		def.setBeanClass(BeanReferenceFactoryBean.class);
		def.setAttribute(GENERATED_REF, true);
		def.setOriginatingBeanDefinition(actualDef);
		def.setSynthetic(true);
		MutablePropertyValues mpv = new MutablePropertyValues();
		mpv.addPropertyValue(TARGET_BEAN_NAME_PROP, beanName);
		def.setPropertyValues(mpv);
		return def;
	}

	protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		ReferenceParsingUtil.checkAvailabilityAndCardinalityDuplication(element, AVAILABILITY, CARDINALITY, context);

		if (defaults == null) {
			defaults = resolveDefaults(element.getOwnerDocument(), context);
		}

		AttributeCallback callback = new ReferenceAttributesCallback();

		parseAttributes(element, builder, new AttributeCallback[] { callback });

		if (!isCardinalitySpecified(builder)) {
			applyDefaultCardinality(builder, defaults);
		}

		parseNestedElements(element, context, builder);

		handleNestedDefinition(element, context, builder);
	}

	private boolean isCardinalitySpecified(BeanDefinitionBuilder builder) {
		return (builder.getBeanDefinition().getPropertyValues().getPropertyValue(AVAILABILITY_PROP) != null);
	}

	/**
	 * If the reference is a nested bean, make it a top-level bean if it's a mandatory dependency. This is done so that
	 * the beans can be discovered at startup and the appCtx can start waiting.
	 * 
	 * @param element
	 * @param context
	 * @param builder
	 */
	protected void handleNestedDefinition(Element element, ParserContext context, BeanDefinitionBuilder builder) {

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
	 * Indicate the bean definition class for this element.
	 * 
	 * @param element
	 * @return
	 */
	protected abstract Class getBeanClass(Element element);

	/**
	 * Apply default cardinality.
	 * 
	 * @param builder
	 * @param defaults
	 */
	protected void applyDefaultCardinality(BeanDefinitionBuilder builder, OsgiDefaultsDefinition defaults) {
		builder.addPropertyValue(AVAILABILITY_PROP, defaults.getAvailability());
	}

	/**
	 * Parse nested elements. In case of a reference definition, this means using the listeners.
	 * 
	 * 
	 * @param element
	 * @param context
	 * @param builder
	 */
	protected void parseNestedElements(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		parseInterfaces(element, context, builder);
		parseListeners(element, getListenerElementName(), context, builder);
	}

	protected String getListenerElementName() {
		return LISTENER;
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
			Set interfaces = parsePropertySetElement(parserContext, element, builder.getBeanDefinition());
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
	protected void parseListeners(Element element, String subElementName, ParserContext context,
			BeanDefinitionBuilder builder) {
		List listeners = DomUtils.getChildElementsByTagName(element, subElementName);

		ManagedList listenersRef = new ManagedList();
		// loop on listeners
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			Element listnr = (Element) iter.next();

			// wrapper target object
			Object target = null;

			// target bean name (in case of a reference)
			String targetName = null;

			// filter elements
			NodeList nl = listnr.getChildNodes();

			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element beanDef = (Element) node;

					// check inline ref
					if (listnr.hasAttribute(REF))
						context.getReaderContext()
								.error("nested bean declaration is not allowed if 'ref' attribute has been specified",
										beanDef);

					target = parsePropertySubElement(context, beanDef, builder.getBeanDefinition());

					// if this is a bean reference (nested <ref>), extract the name
					if (target instanceof RuntimeBeanReference) {
						targetName = ((RuntimeBeanReference) target).getBeanName();
					}
				}
			}

			// extract bind/unbind attributes from <osgi:listener>
			// Element
			MutablePropertyValues vals = new MutablePropertyValues();

			NamedNodeMap attrs = listnr.getAttributes();
			for (int x = 0; x < attrs.getLength(); x++) {
				Attr attribute = (Attr) attrs.item(x);
				String name = attribute.getLocalName();

				// extract ref value
				if (REF.equals(name))
					targetName = attribute.getValue();
				else
					vals.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
			}

			// create serviceListener adapter
			RootBeanDefinition wrapperDef = new RootBeanDefinition(OsgiServiceLifecycleListenerAdapter.class);

			// set the target name (if we have one)
			if (targetName != null)
				vals.addPropertyValue(TARGET_BEAN_NAME_PROP, targetName);
			// else set the actual target
			else
				vals.addPropertyValue(TARGET_PROP, target);

			wrapperDef.setPropertyValues(vals);
			// add listener to list
			listenersRef.add(wrapperDef);
		}

		builder.addPropertyValue(LISTENERS_PROP, listenersRef);
	}

	protected Object parsePropertySubElement(ParserContext context, Element beanDef, BeanDefinition beanDefinition) {
		return context.getDelegate().parsePropertySubElement(beanDef, beanDefinition);
	}

	protected Set parsePropertySetElement(ParserContext context, Element beanDef, BeanDefinition beanDefinition) {
		return context.getDelegate().parseSetElement(beanDef, beanDefinition);
	}

	protected String generateBeanName(String prefix, BeanDefinition def, ParserContext parserContext) {
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		String name = prefix + BeanDefinitionReaderUtils.generateBeanName(def, registry);
		String generated = name;
		int counter = 0;

		while (registry.containsBeanDefinition(name)) {
			generated = name + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + counter;
			counter++;
		}

		return generated;
	}
}