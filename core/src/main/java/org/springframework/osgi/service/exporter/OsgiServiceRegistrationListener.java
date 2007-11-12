/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.service.exporter;

import java.util.Map;

import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;


/**
 * Listener informed of the registration and unregistration of an OSGi service
 * exported by Spring OSGi.
 * 
 * <p>
 * To be used along with {@link OsgiServiceFactoryBean}.
 * </p>
 * 
 * @author Costin Leau
 * 
 */
public interface OsgiServiceRegistrationListener {

	/**
	 * Called when the the service exported has been registered in the OSGi
	 * space.
	 * 
	 * @param service object registered as an OSGi service
	 * @param serviceProperties OSGi service registration properties
	 * @throws Exception exceptions are logged but not propagated to other listeners
	 */
	void registered(Object service, Map serviceProperties) throws Exception;

	/**
	 * Called when the OSGi service has been unregistered (removed from OSGi
	 * space).
	 * 
	 * @param service object unregistered as a service from the OSGi space
	 * @param serviceProperties OSGi service registration properties
	 * @throws Exception exceptions are logged but not propagated to other listeners
	 */
	void unregistered(Object service, Map serviceProperties) throws Exception;

}
