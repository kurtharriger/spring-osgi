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
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.osgi.extender.OsgiServiceDependencyFactory;
import org.springframework.osgi.service.exporter.OsgiServicePropertiesResolver;
import org.springframework.osgi.service.importer.DefaultOsgiServiceDependency;
import org.springframework.osgi.service.importer.OsgiServiceDependency;
import org.springframework.osgi.service.importer.support.Availability;
import org.springframework.osgi.service.importer.support.OsgiServiceCollectionProxyFactoryBean;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Default mandatory importer dependency factory.
 * 
 * <b>Note:</b> To cope with some of the inconsistencies in the Blueprint spec, lazy mandatory importers will not be
 * activated and their configuration will be read directly from the property values (ugh).
 * 
 * @author Costin Leau
 */
public class MandatoryImporterDependencyFactory implements OsgiServiceDependencyFactory {

	private static final String AVAILABILITY_PROP = "availability";
	private static final String INTERFACES_PROP = "interfaces";
	private static final String SERVICE_BEAN_NAME_PROP = "serviceBeanName";
	private static final String FILTER_PROP = "filter";

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
			} else {
				String name = (beans[i].startsWith(BeanFactory.FACTORY_BEAN_PREFIX) ? beans[i].substring(1) : beans[i]);
				if (beanFactory.containsBeanDefinition(name)) {
					BeanDefinition def = beanFactory.getBeanDefinition(name);
					MutablePropertyValues values = def.getPropertyValues();
					// figure out if it's a mandatory bean
					PropertyValue value = values.getPropertyValue(AVAILABILITY_PROP);
					if (value != null && Availability.MANDATORY.equals(value.getValue())) {
						String[] intfs = getInterfaces(values.getPropertyValue(INTERFACES_PROP));
						String beanName = getString(values.getPropertyValue(SERVICE_BEAN_NAME_PROP));
						String filterProp = getString(values.getPropertyValue(FILTER_PROP));

						// create filter
						Filter filter = createFilter(intfs, beanName, filterProp);
						OsgiServiceDependency dependency;
						dependency = new DefaultOsgiServiceDependency(name, filter, true);

						beansCollections.add(dependency);
					}
				}
			}
		}

		return beansCollections;
	}

	private Filter createFilter(String[] intfs, String serviceBeanName, String filter) {
		String filterWithClasses = (!ObjectUtils.isEmpty(intfs) ? OsgiFilterUtils.unifyFilter(intfs, filter) : filter);

		// add the serviceBeanName/Blueprint component name constraint
		String nameFilter;
		if (StringUtils.hasText(serviceBeanName)) {
			StringBuilder nsFilter = new StringBuilder("(|(");
			nsFilter.append(OsgiServicePropertiesResolver.BEAN_NAME_PROPERTY_KEY);
			nsFilter.append("=");
			nsFilter.append(serviceBeanName);
			nsFilter.append(")(");
			nsFilter.append(OsgiServicePropertiesResolver.BLUEPRINT_COMP_NAME);
			nsFilter.append("=");
			nsFilter.append(serviceBeanName);
			nsFilter.append("))");
			nameFilter = nsFilter.toString();
		} else {
			nameFilter = null;
		}

		String filterWithServiceBeanName = filterWithClasses;
		if (nameFilter != null) {
			StringBuilder finalFilter = new StringBuilder();
			finalFilter.append("(&");
			finalFilter.append(filterWithClasses);
			finalFilter.append(nameFilter);
			finalFilter.append(")");
			filterWithServiceBeanName = finalFilter.toString();
		}

		return OsgiFilterUtils.createFilter(filterWithServiceBeanName);
	}

	private String getString(PropertyValue pv) {
		if (pv == null)
			return "";
		Object value = pv.getValue();
		if (value == null) {
			return "";
		}
		if (value instanceof TypedStringValue) {
			return ((TypedStringValue) value).getValue();
		}
		return value.toString();
	}

	private String[] getInterfaces(PropertyValue pv) {
		if (pv == null)
			return new String[0];

		Object value = pv.getValue();

		if (value instanceof Collection) {
			Collection collection = (Collection) value;
			String[] strs = new String[collection.size()];
			int index = 0;
			for (Object obj : collection) {
				if (value instanceof TypedStringValue) {
					strs[index] = ((TypedStringValue) value).getValue();
				} else {
					strs[index] = value.toString();
				}
				index++;
			}
			return strs;
		} else {
			return new String[] { value.toString() };
		}
	}

	private boolean isLazy(ConfigurableListableBeanFactory beanFactory, String beanName) {
		String name = (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX) ? beanName.substring(1) : beanName);
		if (beanFactory.containsBeanDefinition(name)) {
			return beanFactory.getBeanDefinition(name).isLazyInit();
		}
		return false;
	}
}