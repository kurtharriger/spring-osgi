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
package org.springframework.osgi.blueprint.container.support.internal.config;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.core.Ordered;
import org.springframework.osgi.blueprint.config.internal.ParsingUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Simple processor for sorting out cycles between beans. Inspects the construction relationship between beans to
 * provide hints to the container. Specifically, it forces the creation of any beans referred inside the construction
 * through the 'depends-on' attribute on the inspected bean.
 * 
 * @author Costin Leau
 */
public class CycleOrderingProcessor implements BeanFactoryPostProcessor, Ordered {

	public static final String SYNTHETIC_DEPENDS_ON =
			"org.springframework.osgi.blueprint.container.support.internal.config.dependson";

	/** logger */
	private static final Log log = LogFactory.getLog(CycleOrderingProcessor.class);

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		boolean trace = log.isTraceEnabled();

		String[] names = beanFactory.getBeanDefinitionNames();
		for (String name : names) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			if (definition.hasAttribute(ParsingUtils.BLUEPRINT_MARKER_NAME)) {
				ConstructorArgumentValues cArgs = definition.getConstructorArgumentValues();
				if (trace)
					log.trace("Inspecting cycles for (blueprint) bean " + name);

				tag(cArgs.getGenericArgumentValues(), name, definition);
				tag(cArgs.getIndexedArgumentValues().values(), name, definition);
			}
		}
	}

	private void tag(Collection<ValueHolder> values, String name, BeanDefinition definition) {
		boolean trace = log.isTraceEnabled();

		for (ValueHolder value : values) {
			Object val = value.getValue();
			if (val instanceof BeanMetadataElement) {
				if (val instanceof RuntimeBeanReference) {
					String beanName = ((RuntimeBeanReference) val).getBeanName();

					if (trace) {
						log.trace("Adding (cycle breaking) depends-on on " + name + " to " + beanName);
					}

					addSyntheticDependsOn(definition, beanName);
				}
			}
		}
	}

	private void addSyntheticDependsOn(BeanDefinition definition, String beanName) {
		if (StringUtils.hasText(beanName)) {
			String[] dependsOn = definition.getDependsOn();
			if (dependsOn != null && dependsOn.length > 0) {
				for (String dependOn : dependsOn) {
					if (beanName.equals(dependOn)) {
						return;
					}
				}
			}

			// add depends on
			dependsOn = (String[]) ObjectUtils.addObjectToArray(dependsOn, beanName);
			definition.setDependsOn(dependsOn);
			Collection<String> markers = (Collection<String>) definition.getAttribute(SYNTHETIC_DEPENDS_ON);
			if (markers == null) {
				markers = new ArrayList<String>(2);
				definition.setAttribute(SYNTHETIC_DEPENDS_ON, markers);
			}
			markers.add(beanName);
		}
	}

	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}
