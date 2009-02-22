
package org.springframework.osgi.blueprint.namespace;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.service.blueprint.namespace.ComponentDefinitionRegistry;
import org.osgi.service.blueprint.namespace.ComponentNameAlreadyInUseException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.osgi.blueprint.reflect.MetadataFactory;
import org.springframework.util.CollectionUtils;

/**
 * Default {@link ComponentDefinitionRegistry} implementation based on Spring's
 * {@link BeanDefinitionRegistry}.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 * 
 */
public class SpringComponentDefinitionRegistry implements ComponentDefinitionRegistry {

	private final BeanDefinitionRegistry beanRegistry;


	public SpringComponentDefinitionRegistry(BeanDefinitionRegistry beanRegistry) {
		this.beanRegistry = beanRegistry;
	}

	public boolean containsComponentDefinition(String name) {
		return beanRegistry.isBeanNameInUse(name);
	}

	public ComponentMetadata getComponentDefinition(String name) {
		if (!containsComponentDefinition(name)) {
			return null;
		}

		String nameOfBeanWereLookingFor = name;

		// name may be a bean, or an alias to a bean
		// TODO: does an alias needs special treatment?
		if (!beanRegistry.containsBeanDefinition(name)) {
			// must be an alias
			for (String beanName : beanRegistry.getBeanDefinitionNames()) {
				for (String alias : beanRegistry.getAliases(beanName)) {
					if (alias.equals(name)) {
						nameOfBeanWereLookingFor = beanName;
					}
				}
			}
		}

		return MetadataFactory.buildComponentMetadataFor(this.beanRegistry.getBeanDefinition(nameOfBeanWereLookingFor));
	}

	public Set<String> getComponentDefinitionNames() {
		String[] names = beanRegistry.getBeanDefinitionNames();
		Set<String> components = new LinkedHashSet<String>(names.length);
		CollectionUtils.mergeArrayIntoCollection(components, components);
		return Collections.unmodifiableSet(components);
	}

	public void registerComponentDefinition(ComponentMetadata component) throws ComponentNameAlreadyInUseException {
		String name = component.getName();

		if (containsComponentDefinition(name)) {
			throw new ComponentNameAlreadyInUseException(name);
		}

		BeanDefinition beanDefinition = MetadataFactory.buildBeanDefinitionFor(component);
		beanRegistry.registerBeanDefinition(name, beanDefinition);
	}

	public void removeComponentDefinition(String name) {
		beanRegistry.removeBeanDefinition(name);
	}
}