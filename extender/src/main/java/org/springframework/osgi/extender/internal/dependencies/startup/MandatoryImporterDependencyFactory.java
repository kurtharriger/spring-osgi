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

package org.springframework.osgi.extender.internal.dependencies.startup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.osgi.extender.OsgiServiceDependencyFactory;
import org.springframework.osgi.service.importer.DefaultOsgiServiceDependency;
import org.springframework.osgi.service.importer.support.AbstractOsgiServiceImportFactoryBean;

/**
 * Default mandatory importer dependency factory.
 * 
 * @author Costin Leau
 * 
 */
public class MandatoryImporterDependencyFactory implements OsgiServiceDependencyFactory {

	public Collection getServiceDependencies(BundleContext bundleContext, ConfigurableListableBeanFactory beanFactory)
			throws BeansException, InvalidSyntaxException, BundleException {

		String[] beans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
			AbstractOsgiServiceImportFactoryBean.class, true, false);

		List beansCollections = new ArrayList(beans.length);

		for (int i = 0; i < beans.length; i++) {
			String beanName = (beans[i].startsWith(BeanFactory.FACTORY_BEAN_PREFIX) ? beans[i]
					: BeanFactory.FACTORY_BEAN_PREFIX + beans[i]);

			AbstractOsgiServiceImportFactoryBean reference = (AbstractOsgiServiceImportFactoryBean) beanFactory.getBean(beanName);
			beansCollections.add(new DefaultOsgiServiceDependency(beanName, reference.getUnifiedFilter(),
				reference.getCardinality().isMandatory()));
		}

		return beansCollections;
	}
}
