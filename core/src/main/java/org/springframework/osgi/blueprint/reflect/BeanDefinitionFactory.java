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
import java.util.List;

import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.enums.StaticLabeledEnumResolver;
import org.springframework.osgi.service.exporter.support.AutoExport;
import org.springframework.osgi.service.exporter.support.DefaultInterfaceDetector;
import org.springframework.util.StringUtils;

/**
 * Adapter factory that allows translating OSGi Blueprint metadata into Spring's {@link BeanDefinition}.
 * 
 * @author Costin Leau
 */
class BeanDefinitionFactory implements MetadataConstants {

	BeanDefinition buildBeanDefinitionFor(ComponentMetadata metadata) {
		// shortcut (to avoid re-rewrapping)
		if (metadata instanceof SpringComponentMetadata) {
			return ((SpringComponentMetadata) metadata).getBeanDefinition();
		}

		AbstractBeanDefinition definition = buildBeanDefinition(metadata);
		// add common properties
		definition.setAttribute(MetadataConstants.COMPONENT_METADATA_ATTRIBUTE, metadata);
		definition.setAttribute(MetadataConstants.COMPONENT_NAME, metadata.getId());

		// Set<String> dependencies = metadata.;
		// definition.setDependsOn(dependencies.toArray(new String[dependencies.size()]));
		throw new UnsupportedOperationException("move depends on for BeanMetadata");

		// return definition;
	}

	private AbstractBeanDefinition buildBeanDefinition(ComponentMetadata metadata) {

		if (metadata instanceof BeanMetadata) {
			return buildLocalComponent((BeanMetadata) metadata);
		}

		if (metadata instanceof ServiceMetadata) {
			return buildExporter((ServiceMetadata) metadata);
		}

		if (metadata instanceof ServiceReferenceMetadata) {
			if (metadata instanceof RefListMetadata) {
				return buildReferenceCollection((RefListMetadata) metadata);
			}
			if (metadata instanceof ReferenceMetadata) {
				return buildReferenceProxy((ReferenceMetadata) metadata);
			}
		}

		// unknown rich metadata type, unable to perform conversion
		throw new IllegalArgumentException("Unknown metadata type" + metadata.getClass());
	}

	@SuppressWarnings("unchecked")
	private AbstractBeanDefinition buildLocalComponent(BeanMetadata metadata) {
		// add basic definition properties
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(metadata.getClassName()).setInitMethodName(
						metadata.getInitMethodName()).setDestroyMethodName(metadata.getDestroyMethodName())
						.setLazyInit(getLazy(metadata)).setScope(metadata.getScope());

		// add factory-method/factory-bean
		String factoryMethod = metadata.getFactoryMethodName();
		if (StringUtils.hasText(factoryMethod)) {
			builder.setFactoryMethod(factoryMethod);
			Target factory = metadata.getFactoryComponent();
			if (factory != null) {
				builder.getRawBeanDefinition().setFactoryBeanName(((ComponentMetadata) factory).getId());
			}
		}

		// add constructor props
		List<BeanArgument> beanArguments = metadata.getArguments();
		ConstructorArgumentValues cargs = builder.getRawBeanDefinition().getConstructorArgumentValues();

		for (BeanArgument arg : beanArguments) {
			Metadata value = arg.getValue();
			cargs.addIndexedArgumentValue(arg.getIndex(), BeanMetadataElementFactory.buildBeanMetadata(value), arg
					.getValueType());
		}

		// add property values
		Collection<BeanProperty> props = metadata.getProperties();
		MutablePropertyValues pvs = new MutablePropertyValues();

		for (BeanProperty injection : props) {
			pvs.addPropertyValue(injection.getName(), BeanMetadataElementFactory
					.buildBeanMetadata(injection.getValue()));
		}

		return builder.getBeanDefinition();
	}

	private boolean getLazy(ComponentMetadata metadata) {
		return (metadata.getInitialization() == ComponentMetadata.INITIALIZATION_LAZY ? true : false);
	}

	private AbstractBeanDefinition buildExporter(ServiceMetadata metadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(EXPORTER_CLASS);
		// add properties
		builder.addPropertyValue(EXPORTER_RANKING_PROP, metadata.getRanking());
		builder.addPropertyValue(EXPORTER_INTFS_PROP, metadata.getInterfaceNames());
		builder.addPropertyValue(EXPORTER_PROPS_PROP, metadata.getServiceProperties());
		builder.addPropertyValue(EXPORTER_AUTO_EXPORT_PROP, DefaultInterfaceDetector.values()[metadata
				.getAutoExportMode() - 1]);

		BeanMetadataElement beanMetadata = BeanMetadataElementFactory.buildBeanMetadata(metadata.getServiceComponent());
		if (beanMetadata instanceof RuntimeBeanReference) {
			builder.addPropertyValue(EXPORTER_TARGET_BEAN_NAME_PROP, beanMetadata);
		} else {
			builder.addPropertyValue(EXPORTER_TARGET_BEAN_PROP, beanMetadata);
		}

		// FIXME: add registration listeners

		return builder.getBeanDefinition();
	}

	private AbstractBeanDefinition buildReferenceCollection(RefListMetadata metadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MULTI_SERVICE_IMPORTER_CLASS);
		addServiceReferenceProperties(metadata, builder);
		throw new UnsupportedOperationException("not implemented yet");
		// return builder.getBeanDefinition();
	}

	private AbstractBeanDefinition buildReferenceProxy(ReferenceMetadata metadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SINGLE_SERVICE_IMPORTER_CLASS);
		addServiceReferenceProperties(metadata, builder);
		throw new UnsupportedOperationException("not implemented yet");
		// return builder.getBeanDefinition();
	}

	private void addServiceReferenceProperties(ServiceReferenceMetadata referenceMetadata, BeanDefinitionBuilder builder) {
		builder.addPropertyValue(IMPORTER_FILTER_PROP, referenceMetadata.getFilter());
		builder.addPropertyValue(IMPORTER_INTFS_PROP, referenceMetadata.getInterfaceNames());
		// FIXME: add binding listeners
	}
}