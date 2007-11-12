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
package org.springframework.osgi.service.exporter;

import java.util.Map;

/**
 * An OsgiServicePropertiesResolver is responsible for providing the properties
 * that a bean exposed as a service will be published with. It is used mainly as
 * a collaborator of OsgiServiceFactoryBean.
 * 
 * @author Adrian Colyer
 */
public interface OsgiServicePropertiesResolver {

	/**
	 * Predefined property indicating the bean name of an exported Spring
	 * managed object
	 */
	String BEAN_NAME_PROPERTY_KEY = "org.springframework.osgi.beanname";

	/**
	 * Return a map containing the service properties associated with the given
	 * Spring managed bean identified by its name.
	 * 
	 * @param beanName Spring managed bean name
	 * @return map containing the service properties
	 */
	Map getServiceProperties(String beanName);

}
