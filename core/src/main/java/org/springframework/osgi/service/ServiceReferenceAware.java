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
package org.springframework.osgi.service;

import java.util.Map;

import org.osgi.framework.ServiceReference;

/**
 * Aware interface used when importing OSGi services. Gives access to the
 * underlying ServiceReference and its properties.
 * 
 * @author Costin Leau
 * 
 */
public interface ServiceReferenceAware {

	/**
	 * Get access to the raw, underlying service reference.
	 * 
	 * @return underlying service reference.
	 */
	ServiceReference getServiceReference();

	/**
	 * Return a map of service properties.
	 * 
	 * <p/> If needed, the map can be cast to a {@link java.util.Dictionary} class.
	 * 
	 * @return map containing the service properties.
	 */
	Map getServiceProperties();

}
