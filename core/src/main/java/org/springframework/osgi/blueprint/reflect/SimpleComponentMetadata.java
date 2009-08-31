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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.osgi.blueprint.container.support.internal.config.CycleOrderingProcessor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Default {@link ComponentMetadata} implementation based on Spring's {@link BeanDefinition}.
 * 
 * @author Costin Leau
 * 
 */
class SimpleComponentMetadata implements ComponentMetadata {

	private final String name;
	protected final AbstractBeanDefinition beanDefinition;
	private final List<String> dependsOn;
	private final int activation;

	public SimpleComponentMetadata(String name, BeanDefinition definition) {
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
			Collection<String> syntheticDependsOn =
					(Collection<String>) beanDefinition.getAttribute(CycleOrderingProcessor.SYNTHETIC_DEPENDS_ON);

			if (syntheticDependsOn != null) {
				dependencies.removeAll(syntheticDependsOn);
			}

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((beanDefinition == null) ? 0 : beanDefinition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof SimpleComponentMetadata) {
			SimpleComponentMetadata other = (SimpleComponentMetadata) obj;
			if (beanDefinition == null) {
				if (other.beanDefinition != null)
					return false;
			}
			return beanDefinition == other.beanDefinition;
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ComponentMetadata for bean name=");
		sb.append(name);
		sb.append("; activation=");
		sb.append(activation);
		sb.append("; dependsOn=");
		sb.append(dependsOn);
		sb.append("; target definition");
		sb.append(beanDefinition);
		return sb.toString();
	}
}