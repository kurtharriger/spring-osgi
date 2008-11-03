package org.springframework.osgi.blueprint.namespace;

import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.ComponentNameAlreadyInUseException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.osgi.blueprint.reflect.adapters.ComponentMetadata2BeanDefinitionAdapter;
import org.springframework.osgi.blueprint.reflect.adapters.ComponentMetadataFactory;


public class BeanRegistry2ComponentDefinitionRegistryAdapter implements
		ComponentDefinitionRegistry {
	
	private final BeanDefinitionRegistry registry;
	
	public BeanRegistry2ComponentDefinitionRegistryAdapter(BeanDefinitionRegistry reg) {
		this.registry = reg;
	}

	public boolean containsComponentDefinition(String name) {
		return this.registry.isBeanNameInUse(name);
	}

	public ComponentMetadata getComponentDefinition(String name) {
		if (!containsComponentDefinition(name)) {
			return null;
		}
		
		String nameOfBeanWereLookingFor = name;
		
		// name may be a bean, or an alias to a bean
		if (!this.registry.containsBeanDefinition(name)) {
			// must be an alias
			for (String beanName : this.registry.getBeanDefinitionNames()) {
				for (String alias: this.registry.getAliases(beanName)) {
					if (alias.equals(name)) {
						nameOfBeanWereLookingFor = beanName;
					}
				}
			}
		}
		
		return ComponentMetadataFactory.buildComponentMetadataFor(this.registry.getBeanDefinition(nameOfBeanWereLookingFor));
	}


	public String[] getComponentDefinitionNames() {
		return this.registry.getBeanDefinitionNames();
	}

	public void registerComponentDefinition(ComponentMetadata component)
			throws ComponentNameAlreadyInUseException {
		String name = component.getName();

		if (containsComponentDefinition(name)) {
			throw new ComponentNameAlreadyInUseException(name);
		}
		
		for (String alias : component.getAliases()) {
			if (containsComponentDefinition(alias)) {
				throw new ComponentNameAlreadyInUseException(alias);
			}
		}

		BeanDefinition beanDef = new ComponentMetadata2BeanDefinitionAdapter(component);
		this.registry.registerBeanDefinition(name, beanDef);
		for (String alias : component.getAliases()) {
			this.registry.registerAlias(name, alias);
		}
	}

	public void removeComponentDefinition(String name) {
		this.registry.removeBeanDefinition(name);

	}

}
