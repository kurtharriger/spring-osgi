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

package org.springframework.osgi.service.importer.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

/**
 * Base event type used for sending dependencies notifications.
 * 
 * @author Costin Leau
 * 
 */
public abstract class OsgiServiceDependencyEvent extends ApplicationEvent {

	private final String sourceBeanName;
	private final OsgiServiceDependency dependency;


	/**
	 * Constructs a new <code>OsgiServiceDependencyEvent</code> instance.
	 * 
	 * @param source
	 */
	public OsgiServiceDependencyEvent(Object source, String sourceBeanName, OsgiServiceDependency dependency) {
		super(source);
		this.sourceBeanName = sourceBeanName;
		Assert.notNull(dependency);
		this.dependency = dependency;
	}

	/**
	 * Returns the associated source bean name (if present).
	 * 
	 * @return Returns the sourceBeanName
	 */
	public String getSourceBeanName() {
		return sourceBeanName;
	}

	/**
	 * Returns the OSGi service dependency filter for which this event is
	 * triggered.
	 * 
	 * @return Returns the dependencyServiceFilter
	 */
	public OsgiServiceDependency getServiceDependency() {
		return dependency;
	}
}
