/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.osgi.service;

import java.util.Dictionary;

/**
 * Listener tracking binding and unbinding of OSGi services used as normal
 * object references inside Spring OSGi.
 * 
 * @author Costin Leau
 * 
 */
public interface TargetSourceLifecycleListener {

	/**
	 * Called when a service is being binded inside the proxy (be it single or
	 * multi value).
	 * 
	 * @param service the OSGi service instance
	 * @param properties the service properties
	 */
	public void bind(Object service, Dictionary properties) throws Exception;

	/**
	 * Called when a service is being unbinded inside the proxy (be it single or
	 * multi value).
	 * 
	 * @param service the OSGi service instance
	 * @param properties the service properties
	 */
	public void unbind(Object service, Dictionary properties) throws Exception;
}
