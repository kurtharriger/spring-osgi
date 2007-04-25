/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.context.support;

import java.util.Set;

import org.osgi.framework.Filter;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.osgi.context.support.ServiceDependentBundleXmlApplicationContext.Dependency;
import org.springframework.osgi.service.CardinalityOptions;
import org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.util.OsgiFilterUtils;

/**
 * Simple BFPP for detecting OsgiProxyFactoryBean and thus determining the
 * required services.
 * 
 * @author Costin Leau
 * 
 */

public class DependencyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] beans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
			OsgiServiceProxyFactoryBean.class, true, false);

		for (int i = 0; i < beans.length; i++) {
			BeanDefinition definition = beanFactory.getBeanDefinition(beans[i]);
			// parse the definition and determine the osgi filter
			addBeanDependency(definition, dependencies);
		}

	}

	private void addBeanDependency(BeanDefinition beanDefinition, Set dependencies) {

		PropertyValue service = beanDefinition.getPropertyValues().getPropertyValue(
			OsgiServiceProxyFactoryBean.INTERFACE_ATTRIBUTE);
		if (service == null) {
			throw new IllegalStateException("No interface specified for bean [" + beanDefinition + "]");
		}
		PropertyValue serviceFilter = beanDefinition.getPropertyValues().getPropertyValue(
			OsgiServiceProxyFactoryBean.FILTER_ATTRIBUTE);

		String clazz = (String) service.getValue();

		String query = serviceFilter != null ? (String) serviceFilter.getValue() : null;

		String filter = getFactoryBeanOsgiFilter(beanDefinition);

		Filter osgiFilter = OsgiFilterUtils.createFilter(OsgiFilterUtils.unifyFilter(clazz, query));

		PropertyValue cardinality = beanDefinition.getPropertyValues().getPropertyValue(
			OsgiServiceProxyFactoryBean.CARDINALITY_ATTRIBUTE);
		int cardinalityValue = CardinalityOptions.asInt((String) cardinality.getValue());
		dependencies.add(new Dependency(osgiFilter, clazz, cardinalityValue));
	}

	// TODO: implement this
	private String getFactoryBeanOsgiFilter(BeanDefinition definition) {
		throw new UnsupportedOperationException("not implemented yet");
	}
}
