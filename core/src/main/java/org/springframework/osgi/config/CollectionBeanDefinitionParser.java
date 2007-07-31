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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.config.ParserUtils.AttributeCallback;
import org.springframework.osgi.service.importer.OsgiMultiServiceProxyFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * &lt;osgi:collection&gt;, &lt;osgi:list&gt;, &lt;osgi:set&gt;,
 * &lt;osgi:sorted-list&gt;, &lt;osgi:sorted-set&gt; tag parser.
 * 
 * @author Costin Leau
 * 
 */
class CollectionBeanDefinitionParser extends ReferenceBeanDefinitionParser {

	public static final String NESTED_COMPARATOR = "comparator";

	public static final String INLINE_COMPARATOR_REF = "comparator-ref";

	public static final String COLLECTION_TYPE_PROP = "collectionType";

	protected Class getBeanClass(Element element) {
		return OsgiMultiServiceProxyFactoryBean.class;
	}

	protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		ParserUtils.parseCustomAttributes(element, builder, new AttributeCallback() {

			public void process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
				String name = attribute.getLocalName();
				// ref attribute will be handled separately
				if (!INLINE_COMPARATOR_REF.equals(name))
					builder.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
				else {
					builder.addPropertyReference(NESTED_COMPARATOR, StringUtils.trimWhitespace(attribute.getValue()));
				}
			}
		});

		super.parseNestedElements(element, context, builder);

		boolean comparatorRef = element.hasAttribute(INLINE_COMPARATOR_REF);

		// check nested comparator
		Element comparator = DomUtils.getChildElementByTagName(element, NESTED_COMPARATOR);

		Object nestedComparator = null;

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

					nestedComparator = context.getDelegate().parsePropertySubElement(beanDef,
						builder.getBeanDefinition());
				}
			}
		}

		if (nestedComparator != null)
			builder.addPropertyValue(NESTED_COMPARATOR, nestedComparator);

		builder.addPropertyValue(COLLECTION_TYPE_PROP, new Integer(getCollectionType()));
	}

	/**
	 * Indicate the collection parsed by this bean definition. Normally this is
	 * overridden by the Namespace handler and customized appropriately.
	 * 
	 * @return
	 */
	protected int getCollectionType() {
		return OsgiMultiServiceProxyFactoryBean.CollectionOptions.COLLECTION;
	}
}
