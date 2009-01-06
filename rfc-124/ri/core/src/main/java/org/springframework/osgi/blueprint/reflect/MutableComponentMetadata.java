/**
 * 
 */

package org.springframework.osgi.blueprint.reflect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 * @author acolyer
 * 
 */
public abstract class MutableComponentMetadata implements ComponentMetadata {

	private List<String> aliases = new ArrayList<String>();
	private Set<String> dependencies = new LinkedHashSet<String>();
	private String name;


	public MutableComponentMetadata(String name) {
		if (null == name) {
			throw new IllegalArgumentException("name cannot be null");
		}

		this.name = name;
	}

	public String[] getAliases() {
		return this.aliases.toArray(new String[this.aliases.size()]);
	}

	public Set<String> getExplicitDependencies() {
		return Collections.unmodifiableSet(dependencies);
	}

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