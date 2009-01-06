
package org.springframework.osgi.blueprint.namespace;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.ComponentNameAlreadyInUseException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.osgi.blueprint.reflect.adapters.ComponentMetadata2BeanDefinitionAdapter;
import org.springframework.osgi.blueprint.reflect.adapters.ComponentMetadataFactory;
import org.springframework.util.CollectionUtils;

public class BeanRegistry2ComponentDefinitionRegistryAdapter implements ComponentDefinitionRegistry {

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
				for (String alias : this.registry.getAliases(beanName)) {
					if (alias.equals(name)) {
						nameOfBeanWereLookingFor = beanName;
					}
				}
			}
		}

		return ComponentMetadataFactory.buildComponentMetadataFor(this.registry.getBeanDefinition(nameOfBeanWereLookingFor));
	}

	public Set<String> getComponentDefinitionNames() {
		String[] names = this.registry.getBeanDefinitionNames();
		Set<String> components = new LinkedHashSet<String>(names.length);
		CollectionUtils.mergeArrayIntoCollection(components, components);
		return Collections.unmodifiableSet(components);
	}

	public void registerComponentDefinition(ComponentMetadata component) throws ComponentNameAlreadyInUseException {
		String name = component.getName();

		if (containsComponentDefinition(name)) {
			throw new ComponentNameAlreadyInUseException(name);
		}

		BeanDefinition beanDef = new ComponentMetadata2BeanDefinitionAdapter(component);
		this.registry.registerBeanDefinition(name, beanDef);
	}

	public void removeComponentDefinition(String name) {
		this.registry.removeBeanDefinition(name);
	}
}