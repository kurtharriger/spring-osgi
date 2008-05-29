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

package org.springframework.osgi.service.importer.internal.aop;

import org.osgi.framework.Filter;
import org.springframework.core.style.ToStringCreator;
import org.springframework.osgi.service.importer.event.OsgiServiceDependency;
import org.springframework.util.Assert;

/**
 * Default implementation for {@link OsgiServiceDependency}.
 * 
 * @author Costin Leau
 * 
 */
public class DefaultServiceDependency implements OsgiServiceDependency {

	private final String beanName;
	private final Filter filter;
	private final boolean mandatoryService;
	private final String toString;


	/**
	 * Constructs a new <code>DefaultServiceDependency</code> instance.
	 * 
	 * @param beanName
	 * @param filter
	 * @param mandatoryService
	 */
	public DefaultServiceDependency(String beanName, Filter filter, boolean mandatoryService) {
		super();
		this.beanName = beanName;
		Assert.notNull(filter, "the service filter is required");
		this.filter = filter;
		this.mandatoryService = mandatoryService;
		toString = "DependencyService[Name=" + (beanName != null ? beanName : "null") + "][Filter=" + filter
				+ "][Mandatory=" + mandatoryService + "]";
	}

	public String getBeanName() {
		return beanName;
	}

	public Filter getServiceFilter() {
		return filter;
	}

	public boolean isMandatoryService() {
		return mandatoryService;
	}

	public String toString() {
		return toString;
	}
}
