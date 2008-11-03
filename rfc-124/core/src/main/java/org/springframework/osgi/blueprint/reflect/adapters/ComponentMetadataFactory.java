package org.springframework.osgi.blueprint.reflect.adapters;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.osgi.service.importer.support.OsgiServiceCollectionProxyFactoryBean;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;

public class ComponentMetadataFactory {

	/**
	 * Inspect beanDef and return either LocalComponentMetadata, ServiceExportComponentMetadata,
	 * or ServiceReferenceComponentMetadata instance
	 * @param beanDef
	 * @return
	 */
	public static ComponentMetadata buildComponentMetadataFor(BeanDefinition beanDef) {
		if (isOsgiServiceFactoryBean(beanDef)) {
			return new BeanDefinition2ServiceExportComponentMetadataAdapter(beanDef);
		}
		
		if (isServiceReferenceFactoryBean(beanDef)) {
			// TODO: need to further distinguish unary and collection references
			return new BeanDefinition2ServiceReferenceComponentMetadataAdapter(beanDef);
		}
		
		return new BeanDefinition2LocalComponentMetadataAdapter(beanDef);
	}
	
	private static boolean isOsgiServiceFactoryBean(BeanDefinition beanDef) {
		return beanDef.getBeanClassName().equals(OsgiServiceProxyFactoryBean.class.getName());
	}

	private static boolean isServiceReferenceFactoryBean(BeanDefinition def) {
		return ((def.getBeanClassName().equals(OsgiServiceProxyFactoryBean.class.getName())) ||
		        (def.getBeanClassName().equals(OsgiServiceCollectionProxyFactoryBean.class.getName())));
	}

	
}
