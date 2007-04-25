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
 *
 * Created on 26-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi;


/**
 * Exception thrown when an OSGi service obtained via the OsgiServiceProxyFactoryBean
 * is unregistered and attempts to rebind to a suitable replacement have failed.
 * 
 * @see org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean
 * @author Adrian Colyer
 * @since 2.0
 */
public class ServiceUnavailableException extends OsgiServiceException {
	
	private static final long serialVersionUID = 6570972436894394720L;

	public ServiceUnavailableException(String message, Class serviceType, String filter) {
		super(message,serviceType,filter);
	}

}
