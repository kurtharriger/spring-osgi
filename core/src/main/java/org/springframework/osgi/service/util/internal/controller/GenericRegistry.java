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

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

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
	private final Map registry = new WeakHashMap();


	public void putController(Object slave, Object master) {
		registry.put(slave, new WeakReference(master));
	}

	public Object getControllerFor(Object slave) {
		WeakReference ref = (WeakReference) registry.get(slave);
		if (ref != null)
			return (Object) ref.get();
		return null;
	}

	public void removeController(Object slave) {
		registry.remove(slave);
	}
}
