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
import org.springframework.osgi.context.BundleFactoryBean;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * AbstractSingleBeanDefinitionParser that supports the "depends-on" attribute.
 *
 * @author Andy Piper
 */
public class BundleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
	public static final String LAZY_INIT = "lazy-init";

	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		ParserUtils.parseCustomAttributes(element, builder, new ParserUtils.AttributeCallback() {

			public void process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
				builder.addPropertyValue(Conventions.attributeNameToPropertyName(attribute.getLocalName()),
					attribute.getValue());
			}
		});
	}

	protected Class getBeanClass(Element element) {
		return BundleFactoryBean.class;
	}
}
