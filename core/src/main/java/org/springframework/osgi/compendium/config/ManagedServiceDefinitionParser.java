/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.compendium.config;

import java.util.Locale;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.compendium.internal.cm.ManagedServiceInstanceTrackerPostProcessor;
import org.springframework.osgi.config.internal.util.ParserUtils;
import org.springframework.osgi.config.internal.util.AttributeCallback;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Bean definition parser for 'managed-service' element. Configures the
 * infrastructure beans and adds tracking.
 * 
 * @author Costin Leau
 * 
 */
class ManagedServiceDefinitionParser implements BeanDefinitionDecorator {

	/**
	 * Attribute callback dealing with update strategy attribute.
	 * 
	 * @author Costin Leau
	 */
	class UpdateStrategyAttributeCallback implements AttributeCallback {

		public boolean process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
			String name = attribute.getLocalName();
			String value = attribute.getValue();

			// make sure the attribute is
			if (UPDATE_STRATEGY.equals(name)) {
				// convert constant to upper case to let Spring do the
				// conversion
				String val = value.toUpperCase(Locale.ENGLISH).replace('-', '_');
				builder.addPropertyValue(UPDATE_STRATEGY_PROP, val);
				return false;
			}

			return true;
		}
	};


	private static final String UPDATE_STRATEGY = "update-strategy";
	private static final String UPDATE_STRATEGY_PROP = "updateStrategy";


	public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
		BeanDefinition trackingBppDef = createTrackerBpp((Element) node, definition);
		// append the tracked bean name to the generated name for easier debugging 
		String generatedName = parserContext.getReaderContext().generateBeanName(trackingBppDef)
				+ BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + definition.getBeanName();

		parserContext.getRegistry().registerBeanDefinition(generatedName, trackingBppDef);
		return definition;
	}

	private BeanDefinition createTrackerBpp(Element elem, BeanDefinitionHolder definition) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
			ManagedServiceInstanceTrackerPostProcessor.class).setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		builder.addConstructorArgValue(definition.getBeanName());
		ParserUtils.parseCustomAttributes(elem, builder, new UpdateStrategyAttributeCallback());
		return builder.getBeanDefinition();
	}
}
