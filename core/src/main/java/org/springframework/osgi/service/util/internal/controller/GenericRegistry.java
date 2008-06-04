/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.service.util.internal.controller;

import java.util.Map;

import org.springframework.core.CollectionFactory;

/**
 * Internal registry that associates a public object with its internal
 * controller. This allows public APIs to remain clean while allowing framework
 * components in other packages to safely control the public implementations
 * behaviour.
 * 
 * <p/>It's recommended to created dedicated registries for each component to
 * minimize the access done on a certain map.
 * 
 * <p/> This class is thread-safe.
 * 
 * @author Costin Leau
 * 
 */
public class GenericRegistry implements ControllerRegistry {

	/** controller registry */
	private final Map registry = CollectionFactory.createConcurrentMapIfPossible(8);


	public void putController(Object slave, Object master) {
		registry.put(slave, master);
	}

	public Object getControllerFor(Object slave) {
		Object master = registry.get(slave);
		if (master == null)
			throw new IllegalStateException("No master object associated with object " + slave);
		return master;
	}

	public void removeController(Object slave) {
		registry.remove(slave);
	}
}
