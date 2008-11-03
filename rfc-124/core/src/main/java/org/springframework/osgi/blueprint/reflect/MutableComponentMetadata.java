/**
 * 
 */
package org.springframework.osgi.blueprint.reflect;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 * @author acolyer
 *
 */
public abstract class MutableComponentMetadata implements ComponentMetadata {

	private List<String> aliases = new ArrayList<String>();
	private List<String> dependencies = new ArrayList<String>();
	private String name;
	
	public MutableComponentMetadata(String name) {
		if (null == name) {
			throw new IllegalArgumentException("name cannot be null");
		}
		
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.ComponentMetadata#getAliases()
	 */
	public String[] getAliases() {
		return this.aliases.toArray(new String[this.aliases.size()]);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.ComponentMetadata#getExplicitDependencies()
	 */
	public String[] getExplicitDependencies() {
		return this.dependencies.toArray(new String[this.dependencies.size()]);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.ComponentMetadata#getName()
	 */
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		if (null == name) {
			throw new IllegalArgumentException("name cannot be null");
		}
		this.name = name;
	}
	
	public void addAlias(String alias) {
		if (null == alias) {
			throw new IllegalArgumentException("alias cannot be null");
		}
		this.aliases.add(alias);
	}
	
	public void removeAlias(String alias) {
		if (alias != null) {
			this.aliases.remove(alias);
		}
	}
	
	public void addDependency(String componentName) {
		if (null == componentName) {
			throw new IllegalArgumentException("componentName cannot be null");
		}
		this.dependencies.add(componentName);
	}
	
	public void removeDependency(String componentName) {
		if (componentName != null) {
			this.dependencies.remove(componentName);
		}
	}
}
