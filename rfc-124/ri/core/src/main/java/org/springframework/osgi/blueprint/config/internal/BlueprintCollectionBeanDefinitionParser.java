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

package org.springframework.osgi.blueprint.config.internal;

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.config.internal.CollectionBeanDefinitionParser;
import org.springframework.osgi.config.internal.OsgiDefaultsDefinition;
import org.springframework.osgi.config.internal.util.AttributeCallback;
import org.springframework.osgi.config.internal.util.ParserUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Costin Leau
 * 
 */
public abstract class BlueprintCollectionBeanDefinitionParser extends CollectionBeanDefinitionParser {

	private class BlueprintAttrCallback extends BlueprintReferenceAttributeCallback {

		private static final String ORDERING_NAME = "ordering-basis";
		private static final String MEMBER_TYPE = "member-type";
		private static final String SERVICE_ORDER = "service";
		private static final String SERVICE_REF_OREDER = "service-reference";


		public boolean process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
			if (super.process(parent, attribute, builder)) {

				String name = attribute.getLocalName();
				String value = attribute.getValue();

				if (ORDERING_NAME.equals(name)) {
					//builder.addPropertyValue(CARDINALITY_PROP, determineAvailability(value));
					return false;
				}

				if (MEMBER_TYPE.equals(name)) {
					//builder.addPropertyValue(CARDINALITY_PROP, determineAvailability(value));
					return false;
				}

				return true;
			}

			return false;
		}

		@Override
		public Object determineAvailability(String value) {
			return determineCardinality(value);
		}
	}


	@Override
	protected Object determineCardinality(String value) {
		// required
		return super.determineCardinality(value.startsWith("r") ? "1" : "0");
	}

	@Override
	protected OsgiDefaultsDefinition resolveDefaults(Document document) {
		return new BlueprintDefaultsDefinition(document);
	}

	@Override
	protected void parseAttributes(Element element, BeanDefinitionBuilder builder, AttributeCallback[] callbacks) {

		// add BlueprintAttr Callback
		AttributeCallback blueprintCallback = new BlueprintAttrCallback();
		super.parseAttributes(element, builder, ParserUtils.mergeCallbacks(
			new AttributeCallback[] { blueprintCallback }, callbacks));
	}

	@Override
	protected Set parsePropertySetElement(ParserContext context, Element beanDef, BeanDefinition beanDefinition) {
		return ComponentParser.parsePropertySetElement(context, beanDef, beanDefinition);
	}

	@Override
	protected Object parsePropertySubElement(ParserContext context, Element beanDef, BeanDefinition beanDefinition) {
		return ComponentParser.parsePropertySubElement(context, beanDef, beanDefinition);
	}
}