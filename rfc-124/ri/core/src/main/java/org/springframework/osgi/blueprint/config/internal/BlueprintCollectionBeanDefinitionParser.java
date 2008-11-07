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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.osgi.config.internal.CollectionBeanDefinitionParser;
import org.springframework.osgi.config.internal.OsgiDefaultsDefinition;
import org.springframework.osgi.config.internal.util.AttributeCallback;
import org.springframework.osgi.config.internal.util.ParserUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Costin Leau
 * 
 */
public abstract class BlueprintCollectionBeanDefinitionParser extends CollectionBeanDefinitionParser {

	private class BlueprintAttrCallback extends BlueprintReferenceAttributeCallback {

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
}
