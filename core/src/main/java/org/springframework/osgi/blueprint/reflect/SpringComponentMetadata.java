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

package org.springframework.osgi.blueprint.reflect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Default {@link ComponentMetadata} implementation based on Spring's {@link BeanDefinition}.
 * 
 * @author Costin Leau
 * 
 */
public class SpringComponentMetadata implements ComponentMetadata {

	private final String name;
	protected final AbstractBeanDefinition beanDefinition;
	private final List<String> dependsOn;
	private final int activation;

	// FIXME: allow rare-non abstract bean definition as well
	public SpringComponentMetadata(String name, BeanDefinition definition) {
		if (!(definition instanceof AbstractBeanDefinition)) {
			throw new IllegalArgumentException("Unknown bean definition passed in" + definition);
		}
		this.name = name;
		this.beanDefinition = (AbstractBeanDefinition) definition;

		String[] dpdOn = beanDefinition.getDependsOn();
		if (ObjectUtils.isEmpty(dpdOn)) {
			dependsOn = Collections.<String> emptyList();
		} else {
			List<String> dependencies = new ArrayList<String>(dpdOn.length);
			CollectionUtils.mergeArrayIntoCollection(dpdOn, dependencies);
			dependsOn = Collections.unmodifiableList(dependencies);
		}

		if (!StringUtils.hasText(name)) {
			// nested components are always lazy
			activation = ACTIVATION_LAZY;
		} else {
			activation =
					beanDefinition.isSingleton() ? (beanDefinition.isLazyInit() ? ACTIVATION_LAZY : ACTIVATION_EAGER)
							: ACTIVATION_LAZY;
		}
	}

	public BeanDefinition getBeanDefinition() {
		return beanDefinition;
	}

	public String getId() {
		return name;
	}

	public List<String> getDependsOn() {
		return dependsOn;
	}

	public int getActivation() {
		return activation;
	}
}