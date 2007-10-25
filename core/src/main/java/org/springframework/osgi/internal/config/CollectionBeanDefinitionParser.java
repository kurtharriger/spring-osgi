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
package org.springframework.osgi.internal.config;

import java.util.Comparator;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.internal.config.ParserUtils.AttributeCallback;
import org.springframework.osgi.internal.service.collection.CollectionType;
import org.springframework.osgi.internal.service.collection.comparator.OsgiServiceReferenceComparator;
import org.springframework.osgi.service.importer.OsgiMultiServiceProxyFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * &lt;osgi:collection&gt;, &lt;osgi:list&gt;, &lt;osgi:set&gt;, element parser.
 * 
 * @author Costin Leau
 * 
 */
abstract class CollectionBeanDefinitionParser extends ReferenceBeanDefinitionParser {

	private static final String NESTED_COMPARATOR = "comparator";

	private static final String INLINE_COMPARATOR_REF = "comparator-ref";

	private static final String COLLECTION_TYPE_PROP = "collectionType";

	private static final String COMPARATOR_PROPERTY = "comparator";

	private static final String SERVICE_ORDER = "service";

	private static final String SERVICE_REFERENCE_ORDER = "service-reference";

	private static final Comparator SERVICE_REFERENCE_COMPARATOR = new OsgiServiceReferenceComparator();

	private static final String NATURAL = "natural";

	private static final String BASIS = "basis";

	private static final String PROPERTY = "comparator";

	protected Class getBeanClass(Element element) {
		return OsgiMultiServiceProxyFactoryBean.class;
	}

	protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		ParserUtils.parseCustomAttributes(element, builder, new AttributeCallback() {

			public void process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
				String name = attribute.getLocalName();
				String value = attribute.getValue();

				if (CARDINALITY.equals(name)) {
					if (value.startsWith("0"))
						builder.addPropertyValue(MANDATORY, Boolean.FALSE);
					else
						builder.addPropertyValue(MANDATORY, Boolean.TRUE);
				}

				// ref attribute will be handled separately
				else {
					if (!INLINE_COMPARATOR_REF.equals(name))
						builder.addPropertyValue(Conventions.attributeNameToPropertyName(name), value);
					else {
						builder.addPropertyReference(COMPARATOR_PROPERTY, StringUtils.trimWhitespace(value));
					}
				}
			}
		});

		super.parseNestedElements(element, context, builder);

		boolean comparatorRef = element.hasAttribute(INLINE_COMPARATOR_REF);

		// check nested comparator
		Element comparator = DomUtils.getChildElementByTagName(element, NESTED_COMPARATOR);

		Object nestedComparator = null;

		// comparator definition present
		if (comparator != null) {
			// check duplicate nested and inline bean definition
			if (comparatorRef)
				context.getReaderContext().error(
					"nested comparator declaration is not allowed if " + INLINE_COMPARATOR_REF
							+ " attribute has been specified", comparator);

			NodeList nl = comparator.getChildNodes();

			// take only elements
			for (int i = 0; i < nl.getLength(); i++) {
				Node nd = nl.item(i);
				if (nd instanceof Element) {
					Element beanDef = (Element) nd;
					String name = beanDef.getLocalName();
					// check if we have a natural definition
					if (NATURAL.equals(name))
						nestedComparator = parseNaturalComparator(beanDef);
					else
						nestedComparator = context.getDelegate().parsePropertySubElement(beanDef,
							builder.getBeanDefinition());
				}
			}

			if (nestedComparator != null)
				builder.addPropertyValue(COMPARATOR_PROPERTY, nestedComparator);
		}

		if (comparator != null) {
			if (CollectionType.LIST.equals(collectionType()))
				builder.addPropertyValue(COLLECTION_TYPE_PROP, CollectionType.SORTED_LIST.getLabel());

			if (CollectionType.SET.equals(collectionType()))
				builder.addPropertyValue(COLLECTION_TYPE_PROP, CollectionType.SORTED_SET.getLabel());
		}
		else
			builder.addPropertyValue(COLLECTION_TYPE_PROP, collectionType().getLabel());

	}

	protected Comparator parseNaturalComparator(Element element) {
		Comparator comparator = null;
		NamedNodeMap attributes = element.getAttributes();
		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			String name = attribute.getLocalName();
			String value = attribute.getValue();

			if (BASIS.equals(name)) {

				if (SERVICE_REFERENCE_ORDER.equals(value))
					return SERVICE_REFERENCE_COMPARATOR;

				// no comparator means relying on Comparable interface of the
				// services
				else if (SERVICE_ORDER.equals(value))
					return null;
			}

		}

		return comparator;
	}

	/**
	 * Hook used for indicating the main collection type (set/list) on which
	 * this parser applies.
	 * 
	 * @return service collection type
	 */
	protected abstract CollectionType collectionType();
}
