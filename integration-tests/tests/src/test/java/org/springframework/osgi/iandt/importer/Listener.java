/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.osgi.iandt.importer;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Costin Leau
 */
public class Listener {
	final Map<Object, Map> bind = new LinkedHashMap<Object, Map>();
	final Map<Object, Map> unbind = new LinkedHashMap<Object, Map>();

	public void bind(Object service, Map properties) {
		System.out.println("Binding service hash " + System.identityHashCode(service) + " w/ props " + properties);
		bind.put(service, properties);
	}
	
	public void bind(Date service, Map properties) {
		System.out.println("Binding service hash " + System.identityHashCode(service) + " w/ props " + properties);
		bind.put(service, properties);
	}

	public void unbind(Object service, Map properties) {
		System.out.println("Unbinding service hash " + System.identityHashCode(service) + " w/ props " + properties);
		unbind.put(service, properties);
	}
}
