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

package org.springframework.osgi.blueprint.config;

import java.beans.PropertyEditor;
import java.util.List;

import org.osgi.service.blueprint.convert.Converter;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.blueprint.config.internal.ComponentParser;
import org.springframework.osgi.blueprint.convert.CoverterPropertyEditorRegistrar;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser handling &lt;type-converters&gt; elements.
 * 
 * Transforms the {@link Converter converters} into {@link PropertyEditor}
 * through a dedicated {@link PropertyEditorRegistrar registrar} that gets
 * registers through a {@link CustomEditorConfigurer}.
 * 
 * Note that no beans are actually instantiated, the parser generating just the
 * definitions.
 * 
 * @author Costin Leau
 * 
 */
public class TypeConverterBeanDefinitionParser extends AbstractBeanDefinitionParser {

	private static final String EDITOR_CONFIGURER_PROPERTY = "propertyEditorRegistrars";
	public static final String TYPE_CONVERTERS = "type-converters";


	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder registrarDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(CoverterPropertyEditorRegistrar.class);

		List<Element> components = DomUtils.getChildElementsByTagName(element, ComponentParser.BEAN);
		List<Element> componentRefs = DomUtils.getChildElementsByTagName(element,
			BeanDefinitionParserDelegate.REF_ELEMENT);

		ManagedList converterList = new ManagedList(componentRefs.size() + components.size());

		// add components
		for (Element component : components) {
			converterList.add(ComponentParser.parsePropertySubElement(parserContext, component,
				registrarDefinitionBuilder.getBeanDefinition()));
		}
		// followed by bean references
		for (Element componentRef : componentRefs) {
			converterList.add(ComponentParser.parsePropertySubElement(parserContext, componentRef,
				registrarDefinitionBuilder.getBeanDefinition()));
		}
		// add the list to the registrar definition
		registrarDefinitionBuilder.addConstructorArgValue(converterList);

		// build the CustomEditorConfigurer
		return BeanDefinitionBuilder.genericBeanDefinition(CustomEditorConfigurer.class).addPropertyValue(
			EDITOR_CONFIGURER_PROPERTY, registrarDefinitionBuilder.getBeanDefinition()).getBeanDefinition();
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}
}