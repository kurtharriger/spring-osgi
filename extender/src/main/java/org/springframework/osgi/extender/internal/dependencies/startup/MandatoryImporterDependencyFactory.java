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
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.osgi.extender.OsgiServiceDependencyFactory;
import org.springframework.osgi.service.importer.DefaultOsgiServiceDependency;
import org.springframework.osgi.service.importer.OsgiServiceDependency;
import org.springframework.osgi.service.importer.support.Availability;
import org.springframework.osgi.service.importer.support.OsgiServiceCollectionProxyFactoryBean;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;
import org.springframework.util.StringUtils;

/**
 * Default mandatory importer dependency factory.
 * 
 * @author Costin Leau
 */
public class MandatoryImporterDependencyFactory implements OsgiServiceDependencyFactory {

	public Collection<OsgiServiceDependency> getServiceDependencies(BundleContext bundleContext,
			ConfigurableListableBeanFactory beanFactory) throws BeansException, InvalidSyntaxException, BundleException {

		String[] singleBeans =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, OsgiServiceProxyFactoryBean.class,
						true, false);

		String[] collectionBeans =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
						OsgiServiceCollectionProxyFactoryBean.class, true, false);

		String[] beans = StringUtils.concatenateStringArrays(singleBeans, collectionBeans);

		List<OsgiServiceDependency> beansCollections = new ArrayList<OsgiServiceDependency>(beans.length);

		for (int i = 0; i < beans.length; i++) {
			if (!isLazy(beanFactory, beans[i])) {

				String beanName =
						(beans[i].startsWith(BeanFactory.FACTORY_BEAN_PREFIX) ? beans[i]
								: BeanFactory.FACTORY_BEAN_PREFIX + beans[i]);

				SmartFactoryBean<?> reference = beanFactory.getBean(beanName, SmartFactoryBean.class);

				OsgiServiceDependency dependency;
				if (reference instanceof OsgiServiceProxyFactoryBean) {
					OsgiServiceProxyFactoryBean importer = (OsgiServiceProxyFactoryBean) reference;

					dependency =
							new DefaultOsgiServiceDependency(beanName, importer.getUnifiedFilter(),
									Availability.MANDATORY.equals(importer.getAvailability()));
				} else {
					OsgiServiceCollectionProxyFactoryBean importer = (OsgiServiceCollectionProxyFactoryBean) reference;

					dependency =
							new DefaultOsgiServiceDependency(beanName, importer.getUnifiedFilter(),
									Availability.MANDATORY.equals(importer.getAvailability()));
				}

				beansCollections.add(dependency);
			}
		}

		return beansCollections;
	}

	private boolean isLazy(ConfigurableListableBeanFactory beanFactory, String beanName) {
		String name = (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX) ? beanName.substring(1) : beanName);
		if (beanFactory.containsBeanDefinition(name)) {
			return beanFactory.getBeanDefinition(name).isLazyInit();
		}
		return false;
	}
}