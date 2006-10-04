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
 * Created on 25-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
public class OsgiServiceException extends RuntimeException {

	private static final long serialVersionUID = -3159995579765199853L;

	private Class serviceType;
	private String filter;

	public Class getServiceType() {
		return serviceType;
	}
	
	public String getFilter() {
		return filter;
	}
	
	public OsgiServiceException(String message, Class serviceType, String filter) {
		super(message);
		this.serviceType = serviceType;
		this.filter = filter;
	}
}
