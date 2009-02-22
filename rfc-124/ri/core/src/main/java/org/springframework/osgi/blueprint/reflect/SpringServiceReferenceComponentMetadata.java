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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.osgi.service.importer.support.Cardinality;
import org.springframework.util.ObjectUtils;

/**
 * Default {@link ServiceReferenceComponentMetadata} implementation based on
 * Spring's {@link BeanDefinition}.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
// FIXME: the raw bean definition might be unsuitable. If it has been resolved, the instantiated
// bean instance might be better
class SpringServiceReferenceComponentMetadata extends SpringComponentMetadata implements
		ServiceReferenceComponentMetadata {

	private static final String FILTER_PROP = "filter";
	private static final String INTERFACES_PROP = "interfaces";
	private static final String CARDINALITY_PROP = "cardinality";


	/**
	 * Constructs a new <code>SpringServiceReferenceComponentMetadata</code>
	 * instance.
	 * 
	 * @param definition bean definition
	 */
	public SpringServiceReferenceComponentMetadata(BeanDefinition definition) {
		super(definition);
	}

	public Collection<BindingListenerMetadata> getBindingListeners() {
		throw new UnsupportedOperationException();
	}

	public String getComponentName() {
		throw new UnsupportedOperationException();
	}

	public String getFilter() {
		return (String) MetadataUtils.getValue(beanDefinition.getPropertyValues(), FILTER_PROP);
	}

	public Set<String> getInterfaceNames() {
		Class<?>[] classes = (Class<?>[]) MetadataUtils.getValue(beanDefinition.getPropertyValues(), INTERFACES_PROP);

		if (ObjectUtils.isEmpty(classes))
			return Collections.<String> emptySet();

		Set<String> strings = new LinkedHashSet<String>(classes.length);

		for (int i = 0; i < classes.length; i++) {
			strings.add(classes[i].getName());
		}

		return strings;
	}

	public int getServiceAvailabilitySpecification() {
		Cardinality cardinality = (Cardinality) MetadataUtils.getValue(beanDefinition.getPropertyValues(),
			CARDINALITY_PROP);

		return (cardinality == null || cardinality.isMandatory() ? ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY
				: ServiceReferenceComponentMetadata.OPTIONAL_AVAILABILITY);
	}
}