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

/**
 * Dedicated event for OSGi dependencies that are imported in a timed manner.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceDependencyWaitingEvent extends OsgiServiceDependencyEvent {

	private final long timeToWait;


	/**
	 * Constructs a new <code>OsgiServiceDependencyWaitingEvent</code>
	 * instance.
	 * 
	 * @param source
	 * @param sourceBeanName
	 * @param dependencyServiceFilter
	 */
	public OsgiServiceDependencyWaitingEvent(Object source, String sourceBeanName, OsgiServiceDependency dependency,
			long timeToWait) {
		super(source, sourceBeanName, dependency);
		this.timeToWait = timeToWait;
	}

	/**
	 * Returns the time (in milliseconds) the source will wait for the OSGi
	 * service to appear.
	 * 
	 * @return Returns the timeToWait
	 */
	public long getTimeToWait() {
		return timeToWait;
	}
}
