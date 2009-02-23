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
import java.util.Set;

import org.osgi.service.blueprint.reflect.CollectionBasedServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.UnaryServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.enums.StaticLabeledEnumResolver;
import org.springframework.osgi.service.exporter.support.AutoExport;

/**
 * Adapter factory that allows translating OSGi Blueprint metadata into Spring's
 * {@link BeanDefinition}.
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
		Set<String> dependencies = metadata.getExplicitDependencies();
		definition.setDependsOn(dependencies.toArray(new String[dependencies.size()]));
		return definition;
	}

	private AbstractBeanDefinition buildBeanDefinition(ComponentMetadata metadata) {

		if (metadata instanceof LocalComponentMetadata) {
			return buildLocalComponent((LocalComponentMetadata) metadata);
		}

		if (metadata instanceof ServiceExportComponentMetadata) {
			return buildExporter((ServiceExportComponentMetadata) metadata);
		}

		if (metadata instanceof ServiceReferenceComponentMetadata) {
			if (metadata instanceof CollectionBasedServiceReferenceComponentMetadata) {
				return buildReferenceCollection((CollectionBasedServiceReferenceComponentMetadata) metadata);
			}
			if (metadata instanceof UnaryServiceReferenceComponentMetadata) {
				return buildReferenceProxy((UnaryServiceReferenceComponentMetadata) metadata);
			}
		}

		// unknown rich metadata type, unable to perform conversion
		throw new IllegalArgumentException("Unknown metadata type" + metadata.getClass());
	}

	private AbstractBeanDefinition buildLocalComponent(LocalComponentMetadata metadata) {
		// add basic definition properties
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(metadata.getClassName()).setInitMethodName(
			metadata.getInitMethodName()).setDestroyMethodName(metadata.getDestroyMethodName()).setLazyInit(
			metadata.isLazy()).setScope(metadata.getScope());

		// add factory-method/factory-bean
		MethodInjectionMetadata methodMetadata = metadata.getFactoryMethodMetadata();
		if (methodMetadata != null) {
			builder.setFactoryMethod(methodMetadata.getName());
			ReferenceValue factory = (ReferenceValue) metadata.getFactoryComponent();
			if (factory != null) {
				builder.getRawBeanDefinition().setFactoryBeanName(factory.getComponentName());
			}
		}

		// add constructor props
		List<ParameterSpecification> parameterSpecifications = metadata.getConstructorInjectionMetadata().getParameterSpecifications();
		ConstructorArgumentValues cargs = builder.getRawBeanDefinition().getConstructorArgumentValues();

		for (ParameterSpecification param : parameterSpecifications) {
			Value value = param.getValue();
			cargs.addIndexedArgumentValue(param.getIndex(), BeanMetadataElementFactory.buildBeanMetadata(value),
				param.getTypeName());
		}

		// add property values
		Collection<PropertyInjectionMetadata> props = metadata.getPropertyInjectionMetadata();
		MutablePropertyValues pvs = new MutablePropertyValues();

		for (PropertyInjectionMetadata injection : props) {
			pvs.addPropertyValue(injection.getName(),
				BeanMetadataElementFactory.buildBeanMetadata(injection.getValue()));
		}

		return builder.getBeanDefinition();
	}

	private AbstractBeanDefinition buildExporter(ServiceExportComponentMetadata metadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(EXPORTER_CLASS);
		// add properties
		builder.addPropertyValue(EXPORTER_RANKING_PROP, metadata.getRanking());
		builder.addPropertyValue(EXPORTER_INTFS_PROP, metadata.getInterfaceNames());
		builder.addPropertyValue(EXPORTER_PROPS_PROP, metadata.getServiceProperties());
		builder.addPropertyValue(EXPORTER_AUTO_EXPORT_PROP, StaticLabeledEnumResolver.instance().getLabeledEnumByCode(
			AutoExport.class, Integer.valueOf(metadata.getAutoExportMode())));

		BeanMetadataElement beanMetadata = BeanMetadataElementFactory.buildBeanMetadata(metadata.getExportedComponent());
		if (beanMetadata instanceof RuntimeBeanReference) {
			builder.addPropertyValue(EXPORTER_TARGET_BEAN_NAME_PROP, beanMetadata);
		}
		else {
			builder.addPropertyValue(EXPORTER_TARGET_BEAN_PROP, beanMetadata);
		}

		// FIXME: add registration listeners

		return builder.getBeanDefinition();
	}

	private AbstractBeanDefinition buildReferenceCollection(CollectionBasedServiceReferenceComponentMetadata metadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MULTI_SERVICE_IMPORTER_CLASS);
		addServiceReferenceProperties(metadata, builder);
		throw new UnsupportedOperationException("not implemented yet");
		// return builder.getBeanDefinition();
	}

	private AbstractBeanDefinition buildReferenceProxy(UnaryServiceReferenceComponentMetadata metadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SINGLE_SERVICE_IMPORTER_CLASS);
		addServiceReferenceProperties(metadata, builder);
		throw new UnsupportedOperationException("not implemented yet");
		//return builder.getBeanDefinition();
	}

	private void addServiceReferenceProperties(ServiceReferenceComponentMetadata referenceMetadata,
			BeanDefinitionBuilder builder) {
		builder.addPropertyValue(IMPORTER_FILTER_PROP, referenceMetadata.getFilter());
		builder.addPropertyValue(IMPORTER_INTFS_PROP, referenceMetadata.getInterfaceNames());
		// FIXME: add binding listeners
	}
}