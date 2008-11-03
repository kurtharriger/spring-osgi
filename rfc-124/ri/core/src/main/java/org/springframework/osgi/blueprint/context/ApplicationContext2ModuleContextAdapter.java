/**
 * Copyright SpringSource 2008.
 */
package org.springframework.osgi.blueprint.context;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.NoSuchComponentException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.blueprint.reflect.adapters.ComponentMetadataFactory;


/**
 * Adapts Spring's ApplicationContext to the Blueprint Service's ModuleContext interface
 */
public class ApplicationContext2ModuleContextAdapter implements ModuleContext {
	
	private final ConfigurableApplicationContext appCtxt;
	private final BundleContext bundleCtxt;
	
	public ApplicationContext2ModuleContextAdapter(
			ConfigurableApplicationContext aCtxt, BundleContext bCtxt) {
		this.appCtxt = aCtxt;
		this.bundleCtxt = bCtxt;
	}

	/* (non-Javadoc)
	 * @see org.osgi.module.context.ModuleContext#getComponent(java.lang.String)
	 */
	public Object getComponent(String name) throws NoSuchComponentException {
		if (this.appCtxt.containsBean(name)) {
			return this.appCtxt.getBean(name);
		}
		else {
			throw new NoSuchComponentException(name);
		}
	}

	/* (non-Javadoc)
	 * @see org.osgi.module.context.ModuleContext#getComponentMetadata(java.lang.String)
	 */
	public ComponentMetadata getComponentMetadata(String name) throws NoSuchComponentException {
		if (this.appCtxt.containsBeanDefinition(name)) {
			throw new NoSuchComponentException(name);
		}
		
		BeanDefinition beanDef = getBeanDefinition(name);
		
		return ComponentMetadataFactory.buildComponentMetadataFor(beanDef);
	}


	/* (non-Javadoc)
	 * @see org.osgi.module.context.ModuleContext#getComponentNames()
	 */
	public String[] getComponentNames() {
		return this.appCtxt.getBeanDefinitionNames();
	}

	/* (non-Javadoc)
	 * @see org.osgi.module.context.ModuleContext#getExportedServicesMetadata()
	 */
	public ServiceExportComponentMetadata[] getExportedServicesMetadata() {
		List<ServiceExportComponentMetadata> serviceMetadata = 
			new ArrayList<ServiceExportComponentMetadata>();
		for (ComponentMetadata metadata : getComponentMetadataForAllComponents()) {
			if (metadata instanceof ServiceExportComponentMetadata) {
				serviceMetadata.add((ServiceExportComponentMetadata)metadata);
			}
		}
		return serviceMetadata.toArray(new ServiceExportComponentMetadata[serviceMetadata.size()]);
	}

	/* (non-Javadoc)
	 * @see org.osgi.module.context.ModuleContext#getReferencedServicesMetadata()
	 */
	public ServiceReferenceComponentMetadata[] getReferencedServicesMetadata() {
		List<ServiceReferenceComponentMetadata> references = new ArrayList<ServiceReferenceComponentMetadata>();
		for (ComponentMetadata metadata : getComponentMetadataForAllComponents()) {
			if (metadata instanceof ServiceReferenceComponentMetadata) {
				references.add((ServiceReferenceComponentMetadata)metadata);
			}
		}
		return references.toArray(new ServiceReferenceComponentMetadata[references.size()]);
	}

	/* (non-Javadoc)
	 * @see org.osgi.module.context.ModuleContext#getLocalComponentsMetadata()
	 */
	public LocalComponentMetadata[] getLocalComponentsMetadata() {
		List<LocalComponentMetadata> localMetadata = new ArrayList<LocalComponentMetadata>();
		for (ComponentMetadata metadata : getComponentMetadataForAllComponents()) {
			if (metadata instanceof LocalComponentMetadata) {
				localMetadata.add((LocalComponentMetadata)metadata);
			}
		}
		return localMetadata.toArray(new LocalComponentMetadata[localMetadata.size()]);
	}

	public BundleContext getBundleContext() {
		return this.bundleCtxt;
	}

	// implementation helper methods...
	
	private BeanDefinition getBeanDefinition(String name) {
		ConfigurableListableBeanFactory factory = this.appCtxt.getBeanFactory();
		BeanDefinition beanDef = factory.getBeanDefinition(name);
		return beanDef;
	}

	// could be more efficient and cache, if we knew when the cache was stale
	private List<ComponentMetadata> getComponentMetadataForAllComponents() {
		List<ComponentMetadata> metadata = new ArrayList<ComponentMetadata>();
		for (String beanName : getComponentNames()) {
			metadata.add(ComponentMetadataFactory.buildComponentMetadataFor(getBeanDefinition(beanName)));
		}
		return metadata;
	}
}
