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

import java.util.StringTokenizer;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * @author Andy Piper
 * @author Costin Leau
 * @since 2.1
 */
public class ParserUtils {

	public static void parseDependsOn(Attr attribute, BeanDefinitionBuilder builder) {
		for (StringTokenizer dependents = new StringTokenizer(attribute.getValue(), ", "); dependents.hasMoreElements();) {
			String dep = (String) dependents.nextElement();
			if (StringUtils.hasText(dep)) {
				builder.addDependsOn(dep);
			}
		}
	}

	/**
	 * Parse default attributes such as ID, LAZY-INIT, DEPENDS-ON.
	 * 
	 * @param element
	 * @param builder
	 */
	public static void parseCustomAttributes(Element element, BeanDefinitionBuilder builder, AttributeCallback callback) {
		NamedNodeMap attributes = element.getAttributes();

		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			String name = attribute.getLocalName();

			if (BeanDefinitionParserDelegate.ID_ATTRIBUTE.equals(name)) {
				continue;
			}
			else if (BeanDefinitionParserDelegate.DEPENDS_ON_ATTRIBUTE.equals(name)) {
				builder.getBeanDefinition().setDependsOn(
						(StringUtils.tokenizeToStringArray(attribute.getValue(),
								BeanDefinitionParserDelegate.BEAN_NAME_DELIMITERS)));
			}
			else if (BeanDefinitionParserDelegate.LAZY_INIT_ATTRIBUTE.equals(name)) {
				builder.setLazyInit(Boolean.getBoolean(attribute.getValue()));
			}
			else {
				callback.process(element, attribute, builder);
			}
		}
	}

	/**
	 * Simple callback used for parsing attributes that have not been covered by
	 * the skipBeanAttributes method.
	 * 
	 * @author Costin Leau
	 * 
	 */
	public static interface AttributeCallback {

		public void process(Element parent, Attr attribute, BeanDefinitionBuilder builder);
	}

}
