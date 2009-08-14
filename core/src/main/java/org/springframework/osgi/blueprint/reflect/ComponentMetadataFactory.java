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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReferenceFactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.osgi.config.internal.AbstractReferenceDefinitionParser;

/**
 * Internal class used for adapting Spring's bean definition to OSGi Blueprint metadata. Used by {@link MetadataFactory}
 * which acts as a facade.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
class ComponentMetadataFactory implements MetadataConstants {

	private static final String BEAN_REF_FB_CLASS_NAME = BeanReferenceFactoryBean.class.getName();
	private static final String GENERATED_REF = AbstractReferenceDefinitionParser.GENERATED_REF;
	private static final String PROMOTED_REF = AbstractReferenceDefinitionParser.PROMOTED_REF;
	private static final String REGEX =
			"\\.org\\.springframework\\.osgi\\.service\\.importer\\.support\\.OsgiService(?:Collection)*ProxyFactoryBean#\\d+#\\d+";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	private static final String GENERATED_END = "#generated";
	private static final String GENERATED_START = ".org.springframework.osgi.service.importer.support.OsgiService";
	private static final String GENERATED_MIDDLE = "ProxyFactoryBean#";

	/**
	 * Builds a component metadata from the given bean definition.
	 * 
	 * @param name bean name
	 * @param beanDefinition
	 * @return
	 */
	static ComponentMetadata buildMetadata(String name, BeanDefinition beanDefinition) {
		// shortcut (to avoid re-re-wrapping)
		Object metadata = beanDefinition.getAttribute(COMPONENT_METADATA_ATTRIBUTE);
		if (metadata instanceof ComponentMetadata)
			return (ComponentMetadata) metadata;

		// if no name has been given, look for one
		if (name == null) {
			name = (String) beanDefinition.getAttribute(COMPONENT_NAME);
		}

		if (isServiceExporter(beanDefinition)) {
			return new SpringServiceExportComponentMetadata(name, beanDefinition);
		}

		if (isSingleServiceImporter(beanDefinition)) {
			return new SpringReferenceMetadata(name, beanDefinition);
		}
		if (isCollectionImporter(beanDefinition)) {
			return new SpringReferenceListMetadata(name, beanDefinition);
		}

		BeanDefinition original = unwrapImporterReference(beanDefinition);
		if (original != null) {
			return buildMetadata(null, original);
		}

		if (isEnvironmentManager(beanDefinition)) {
			return new EnvironmentManagerMetadata(name);
		}

		return new SpringBeanMetadata(name, beanDefinition);
	}

	private static boolean isServiceExporter(BeanDefinition beanDefinition) {
		return checkBeanDefinitionClassCompatibility(beanDefinition, EXPORTER_CLASS);
	}

	private static boolean isSingleServiceImporter(BeanDefinition beanDefinition) {
		return checkBeanDefinitionClassCompatibility(beanDefinition, SINGLE_SERVICE_IMPORTER_CLASS);
	}

	private static boolean isCollectionImporter(BeanDefinition beanDefinition) {
		return checkBeanDefinitionClassCompatibility(beanDefinition, MULTI_SERVICE_IMPORTER_CLASS);
	}

	static BeanDefinition unwrapImporterReference(BeanDefinition beanDefinition) {
		if (BEAN_REF_FB_CLASS_NAME.equals(beanDefinition.getBeanClassName())) {
			// check special DM case of nested mandatory
			// references being promoted to top level beans
			if (beanDefinition instanceof AbstractBeanDefinition) {
				AbstractBeanDefinition abd = (AbstractBeanDefinition) beanDefinition;
				if (abd.isSynthetic() && abd.hasAttribute(GENERATED_REF)) {
					BeanDefinition actual = abd.getOriginatingBeanDefinition();
					return actual;
				}
			}
		}

		return null;
	}

	private static boolean isEnvironmentManager(BeanDefinition beanDefinition) {
		return checkBeanDefinitionClassCompatibility(beanDefinition, ENV_FB_CLASS);
	}

	private static boolean checkBeanDefinitionClassCompatibility(BeanDefinition definition, Class<?> clazz) {
		if (definition instanceof AbstractBeanDefinition) {
			AbstractBeanDefinition abstractDefinition = (AbstractBeanDefinition) definition;
			if (abstractDefinition.hasBeanClass()) {
				Class<?> beanClass = abstractDefinition.getBeanClass();
				return clazz.isAssignableFrom(beanClass);
			}
		}
		return (clazz.getName().equals(definition.getBeanClassName()));
	}

	static Collection<ComponentMetadata> buildNestedMetadata(String beanName, BeanDefinition beanDefinition) {
		List<ComponentMetadata> col = new ArrayList<ComponentMetadata>(4);
		processBeanDefinition(beanDefinition, col);
		// remove the first definition
		col.remove(0);
		return col;
	}

	private static void processBeanMetadata(BeanMetadataElement metadata, Collection<ComponentMetadata> to) {
		if (metadata instanceof BeanDefinition) {
			processBeanDefinition((BeanDefinition) metadata, to);
		}

		else if (metadata instanceof BeanDefinitionHolder) {
			BeanDefinitionHolder bh = (BeanDefinitionHolder) metadata;
			processBeanDefinition(bh.getBeanDefinition(), to);
		}

		else if (metadata instanceof Mergeable && metadata instanceof Iterable) {
			processIterable((Iterable) metadata, to);
		}
	}

	private static void processBeanDefinition(BeanDefinition definition, Collection<ComponentMetadata> to) {
		to.add(buildMetadata(null, definition));

		// start with constructors
		ConstructorArgumentValues cavs = definition.getConstructorArgumentValues();
		// generic values
		List<ValueHolder> genericValues = cavs.getGenericArgumentValues();
		for (ValueHolder valueHolder : genericValues) {
			Object value = MetadataUtils.getValue(valueHolder);
			if (value instanceof BeanMetadataElement) {
				processBeanMetadata((BeanMetadataElement) value, to);
			}
		}
		// indexed ones
		Map<Integer, ValueHolder> indexedValues = cavs.getIndexedArgumentValues();
		for (ValueHolder valueHolder : indexedValues.values()) {
			Object value = MetadataUtils.getValue(valueHolder);
			if (value instanceof BeanMetadataElement) {
				processBeanMetadata((BeanMetadataElement) value, to);
			}
		}

		// now property values
		PropertyValues pvs = definition.getPropertyValues();
		for (PropertyValue pv : pvs.getPropertyValues()) {
			Object value = MetadataUtils.getValue(pv);
			if (value instanceof BeanMetadataElement) {
				processBeanMetadata((BeanMetadataElement) value, to);
			}
		}
	}

	private static void processIterable(Iterable iterableMetadata, Collection<ComponentMetadata> to) {
		for (Object value : iterableMetadata) {
			if (value instanceof BeanMetadataElement) {
				processBeanMetadata((BeanMetadataElement) value, to);
			}
		}
	}

	public List<ComponentMetadata> buildComponentMetadataFor(ConfigurableListableBeanFactory factory) {
		List<ComponentMetadata> metadata = new ArrayList<ComponentMetadata>();
		String[] components = factory.getBeanDefinitionNames();

		for (String beanName : components) {
			BeanDefinition definition = factory.getBeanDefinition(beanName);

			// filter generated definitions
			if (!definition.hasAttribute(PROMOTED_REF)) {
				// add metadata for top-level definitions
				metadata.add(MetadataFactory.buildComponentMetadataFor(beanName, definition));
				// look for nested ones
				metadata.addAll(MetadataFactory.buildNestedComponentMetadataFor(beanName, definition));
			}
		}

		return metadata;
	}

	// eliminate the names of promoted importers
	public Set<String> filterIds(Set<String> components) {
		// search for pattern "
		// .org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean#N#N and
		// .org.springframework.osgi.service.importer.support.OsgiServiceCollectionProxyFactoryBean#N#N

		Set<String> filtered = new LinkedHashSet<String>(components.size());

		for (String string : components) {
			if (!(string.startsWith(GENERATED_START) && string.endsWith(GENERATED_END) && string
					.contains(GENERATED_MIDDLE))) {
				filtered.add(string);
			}
		}

		return filtered;
	}
}