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

package org.springframework.osgi.blueprint.config.internal;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.config.internal.OsgiDefaultsDefinition;
import org.springframework.osgi.config.internal.ServiceBeanDefinitionParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Costin Leau
 */
public class BlueprintServiceDefinitionParser extends ServiceBeanDefinitionParser {

	@Override
	protected Map<?, ?> parsePropertyMapElement(ParserContext context, Element beanDef, BeanDefinition beanDefinition) {
		return BlueprintParser.parsePropertyMapElement(context, beanDef, beanDefinition);
	}

	@Override
	protected Set<?> parsePropertySetElement(ParserContext context, Element beanDef, BeanDefinition beanDefinition) {
		return BlueprintParser.parsePropertySetElement(context, beanDef, beanDefinition);
	}

	@Override
	protected Object parsePropertySubElement(ParserContext context, Element beanDef, BeanDefinition beanDefinition) {
		return BlueprintParser.parsePropertySubElement(context, beanDef, beanDefinition);
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		return ParsingUtils.resolveId(element, definition, parserContext, shouldGenerateId(),
				shouldGenerateIdAsFallback());
	}

	@Override
	protected OsgiDefaultsDefinition resolveDefaults(Document document, ParserContext parserContext) {
		return new BlueprintDefaultsDefinition(document, parserContext);
	}

	@Override
	protected void postProcessListenerDefinition(BeanDefinition wrapperDef) {
		wrapperDef.getPropertyValues().addPropertyValue("blueprintCompliant", true);
	}

	@Override
	protected void applyDefaults(ParserContext parserContext, OsgiDefaultsDefinition defaults,
			BeanDefinitionBuilder builder) {
		super.applyDefaults(parserContext, defaults, builder);
		if (defaults instanceof BlueprintDefaultsDefinition) {
			BlueprintDefaultsDefinition defs = (BlueprintDefaultsDefinition) defaults;
			if (defs.getDefaultInitialization()) {
				builder.setLazyInit(defs.getDefaultInitialization());
			}
		}
	}
}
