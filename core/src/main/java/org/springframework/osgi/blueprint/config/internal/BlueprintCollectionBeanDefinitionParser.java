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

import java.util.Set;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.config.internal.CollectionBeanDefinitionParser;
import org.springframework.osgi.config.internal.OsgiDefaultsDefinition;
import org.springframework.osgi.config.internal.util.AttributeCallback;
import org.springframework.osgi.config.internal.util.ParserUtils;
import org.springframework.osgi.service.importer.support.CollectionType;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Costin Leau
 * 
 */
public abstract class BlueprintCollectionBeanDefinitionParser extends CollectionBeanDefinitionParser {

	private static final String REFERENCE_LISTENER = "reference-listener";

	@Override
	protected OsgiDefaultsDefinition resolveDefaults(Document document, ParserContext parserContext) {
		return new BlueprintDefaultsDefinition(document, parserContext);
	}

	@Override
	protected void parseAttributes(Element element, BeanDefinitionBuilder builder, AttributeCallback[] callbacks,
			OsgiDefaultsDefinition defaults) {

		// add BlueprintAttr Callback
		AttributeCallback blueprintCallback = new BlueprintReferenceAttributeCallback();
		super.parseAttributes(element, builder, ParserUtils.mergeCallbacks(
				new AttributeCallback[] { blueprintCallback }, callbacks), defaults);
	}

	@Override
	protected Set parsePropertySetElement(ParserContext context, Element beanDef, BeanDefinition beanDefinition) {
		return BlueprintParser.parsePropertySetElement(context, beanDef, beanDefinition);
	}

	@Override
	protected Object parsePropertySubElement(ParserContext context, Element beanDef, BeanDefinition beanDefinition) {
		return BlueprintParser.parsePropertySubElement(context, beanDef, beanDefinition);
	}

	@Override
	protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		super.doParse(element, context, builder);
		builder.addPropertyValue("useBlueprintExceptions", true);
		builder.addPropertyValue("blueprintCompliant", true);
	}

	@Override
	protected String getListenerElementName() {
		return REFERENCE_LISTENER;
	}

	@Override
	protected String generateBeanName(String id, BeanDefinition def, ParserContext parserContext) {
		return super.generateBeanName(ParsingUtils.BLUEPRINT_GENERATED_NAME_PREFIX + id, def, parserContext);
	}

	@Override
	protected CollectionType collectionType() {
		return null;
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

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		String id = element.getAttribute(ID_ATTRIBUTE);
		if (!StringUtils.hasText(id)) {
			id = generateBeanName("", definition, parserContext);
		}
		return id;
	}
}