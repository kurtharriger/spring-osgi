/*
 * Copyright 2008 the original author or authors.
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
