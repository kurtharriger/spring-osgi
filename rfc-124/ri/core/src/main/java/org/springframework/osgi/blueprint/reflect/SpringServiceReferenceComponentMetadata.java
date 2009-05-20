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

import org.osgi.service.blueprint.reflect.Listener;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.osgi.service.importer.support.Cardinality;

/**
 * Default {@link ServiceReferenceComponentMetadata} implementation based on Spring's {@link BeanDefinition}.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
abstract class SpringServiceReferenceComponentMetadata extends SpringComponentMetadata implements
		ServiceReferenceMetadata {

	private static final String FILTER_PROP = "filter";
	private static final String INTERFACES_PROP = "interfaces";
	private static final String CARDINALITY_PROP = "cardinality";
	private static final String SERVICE_NAME_PROP = "serviceBeanName";
	private static final String LISTENERS_PROP = "listeners";

	private final String componentName;
	private final String filter;
	private final int availability;
	private final List<String> interfaces;
	private final Collection<Listener> listeners;

	/**
	 * Constructs a new <code>SpringServiceReferenceComponentMetadata</code> instance.
	 * 
	 * @param name bean name
	 * @param definition bean definition
	 */
	public SpringServiceReferenceComponentMetadata(String name, BeanDefinition definition) {
		super(name, definition);

		MutablePropertyValues pvs = beanDefinition.getPropertyValues();
		componentName = (String) MetadataUtils.getValue(pvs, SERVICE_NAME_PROP);
		filter = (String) MetadataUtils.getValue(pvs, FILTER_PROP);

		Cardinality cardinality = (Cardinality) MetadataUtils.getValue(pvs, CARDINALITY_PROP);

		availability = (cardinality == null || cardinality.isMandatory() ? ServiceReferenceMetadata.AVAILABILITY_MANDATORY
				: ServiceReferenceMetadata.AVAILABILITY_OPTIONAL);

		// interfaces
		Object value = MetadataUtils.getValue(pvs, INTERFACES_PROP);

		List<String> intfs = new ArrayList<String>(4);
		// interface attribute used
		if (value instanceof String) {
			intfs.add((String) value);
		}

		else {
			if (value instanceof Collection) {
				Collection<TypedStringValue> values = (Collection) value;

				for (TypedStringValue tsv : values) {
					intfs.add(tsv.getValue());
				}
			}
		}
		interfaces = Collections.unmodifiableList(intfs);

		// listeners
		List<Listener> foundListeners = new ArrayList<Listener>(4);
		List<? extends AbstractBeanDefinition> listenerDefinitions = (List) MetadataUtils.getValue(pvs, LISTENERS_PROP);

		if (listenerDefinitions != null) {
			for (AbstractBeanDefinition beanDef : listenerDefinitions) {
				foundListeners.add(new SimpleListenerMetadata(beanDef));
			}
		}

		listeners = Collections.unmodifiableCollection(foundListeners);
	}

	public int getAvailability() {
		return availability;
	}

	public String getComponentName() {
		return componentName;
	}

	public String getFilter() {
		return filter;
	}

	public List<String> getInterfaceNames() {
		return interfaces;
	}

	public Collection<Listener> getServiceListeners() {
		return listeners;
	}
}