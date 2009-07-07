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
package org.springframework.osgi.config.internal.util;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * @author Costin Leau
 */
public class BlueprintAttributeCallback implements AttributeCallback {

	private static final String ACTIVATION_ATTR = "activation";
	private static final String LAZY_ACTIVATION = "lazy";

	public boolean process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
		String name = attribute.getLocalName();
		String value = attribute.getValue();

		if (ACTIVATION_ATTR.equals(name) && StringUtils.hasText(value) && LAZY_ACTIVATION.equalsIgnoreCase(value)) {
			builder.setLazyInit(true);
			return false;
		}

		return true;
	}
}
