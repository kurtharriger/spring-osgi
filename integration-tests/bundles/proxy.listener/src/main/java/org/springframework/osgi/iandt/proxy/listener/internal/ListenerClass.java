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
package org.springframework.osgi.iandt.proxy.listener.internal;

import java.util.Map;

import org.springframework.osgi.iandt.proxy.listener.Listener;

/**
 * @author Costin Leau
 */
public class ListenerClass implements Listener {

	public void bind(Object service, Map props) {
		System.out.println("Bind service " + service + " w/ props "+ props);
	}

	public void unbind(Object service, Map props) {
		System.out.println("Unbind service " + service + " w/ props "+ props);
	}
}
