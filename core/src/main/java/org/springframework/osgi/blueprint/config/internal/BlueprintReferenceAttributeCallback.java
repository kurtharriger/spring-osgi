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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.osgi.config.internal.util.AttributeCallback;
import org.springframework.osgi.config.internal.util.ReferenceParsingUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * RFC124/Blueprint specific attributes that need to be converted to Spring DM.
 * 
 * @author Costin Leau
 */
public class BlueprintReferenceAttributeCallback implements AttributeCallback {

	private static final String AVAILABILITY = "availability";

	private static final String SERVICE_BEAN_NAME_PROP = "serviceBeanName";

	private static final String COMPONENT_NAME = "component-name";

	public boolean process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
		String name = attribute.getLocalName();
		String value = attribute.getValue();

		if (AVAILABILITY.equals(name)) {
			builder.addPropertyValue(AVAILABILITY, ReferenceParsingUtil.determineAvailability(value));
			return false;
		}

		else if (COMPONENT_NAME.equals(name)) {
			builder.addPropertyValue(SERVICE_BEAN_NAME_PROP, value);
			return false;
		}

		return true;
	}
}