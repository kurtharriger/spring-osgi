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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Default {@link ComponentMetadata} implementation based on Spring's
 * {@link BeanDefinition}.
 * 
 * @author Costin Leau
 * 
 */
public class SpringComponentMetadata implements ComponentMetadata {

	protected final String name;
	protected final AbstractBeanDefinition beanDefinition;


	// FIXME: allow rare-non abstract bean definition as well 
	public SpringComponentMetadata(String name, BeanDefinition definition) {
		if (!(definition instanceof AbstractBeanDefinition)) {
			throw new IllegalArgumentException("Unknown bean definition passed in" + definition);
		}
		this.name = name;
		this.beanDefinition = (AbstractBeanDefinition) definition;
	}

	public BeanDefinition getBeanDefinition() {
		return beanDefinition;
	}

	public Set<String> getExplicitDependencies() {
		String[] dependsOn = beanDefinition.getDependsOn();
		if (ObjectUtils.isEmpty(dependsOn)) {
			return Collections.<String> emptySet();
		}
		Set<String> dependencies = new LinkedHashSet<String>(dependsOn.length);
		CollectionUtils.mergeArrayIntoCollection(dependsOn, dependencies);
		return Collections.unmodifiableSet(dependencies);
	}

	public String getName() {
		return name;
	}
}