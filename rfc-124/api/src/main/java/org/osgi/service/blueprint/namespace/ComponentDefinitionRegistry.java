package org.osgi.service.blueprint.namespace;

import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 * A registry of the component definitions for a given context.
 * 
 * Implementations of ComponentDefinitionRegistry are required to support
 * concurrent access. The state of a component registry
 * may change between invocations on the same thread. For example, a single
 * thread invoking containsComponentDefinition("foo") and getting a return value
 * of 'true' may see a return value of null on a subsequent call to
 * getComponentDefinition("foo") if another thread has removed the component
 * definition in the meantime. 
 */
public interface ComponentDefinitionRegistry {

	/**
	 * Returns true iff the registry contains a component definition with
	 * the given name as its component name or as an alias. 
	 */
	boolean containsComponentDefinition(String name);
	
	/**
	 * Get the component definition for the component with the given name
	 * or alias.
	 *
	 * @return the matching component definition if present, or null if no
	 * component with a matching name or alias is present.
	 */
	ComponentMetadata getComponentDefinition(String name);
	
	/**
	 * Get the names of all the registered components.
	 * 
	 * @return an immutable set (of Strings) containing the primary names of all registered components.
	 */
	Set getComponentDefinitionNames();
	
	/**
	 * Register a new component definition.
	 * 
	 * @throws ComponentNameAlreadyInUseException if the name or one of the aliases of the 
	 * component definition to be registered is already in use by an existing component
	 * definition.
	 */
	void registerComponentDefinition(ComponentMetadata component) throws ComponentNameAlreadyInUseException;
	
	/**
	 * Remove a component definition from the registry. If no matching component
	 * is present then this operation does nothing.
	 * 
	 * @param name the name (or an alias) of the component to be removed.
	 */
	void removeComponentDefinition(String name);
	
}
