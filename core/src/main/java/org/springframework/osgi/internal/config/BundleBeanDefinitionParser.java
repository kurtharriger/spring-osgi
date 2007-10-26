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
package org.springframework.osgi.internal.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.osgi.context.support.BundleFactoryBean;
import org.springframework.osgi.internal.config.ParserUtils.AttributeCallback;
import org.w3c.dom.Element;

/**
 * BundleFactoryBean definition.
 * 
 * @author Andy Piper
 * @author Costin Leau
 */
class BundleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		ParserUtils.parseCustomAttributes(element, builder, (AttributeCallback) null);
	}

	protected Class getBeanClass(Element element) {
		return BundleFactoryBean.class;
	}
}
