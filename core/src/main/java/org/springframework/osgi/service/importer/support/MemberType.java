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
package org.springframework.osgi.service.importer.support;

import org.osgi.framework.ServiceReference;

/**
 * The type of the service collection. The members can be either service instances (proxies to the actual service
 * objects) or the appropriate service references.
 * 
 * @author Costin Leau
 */
public enum MemberType {

	/**
	 * Indicates proxies for the imported services.
	 */
	SERVICE_OBJECT,

	/**
	 * Indicates {@link ServiceReference}s matching the target service type
	 */
	SERVICE_REFERENCE;
}
