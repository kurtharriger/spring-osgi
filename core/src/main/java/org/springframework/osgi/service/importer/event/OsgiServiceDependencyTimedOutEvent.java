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
 * @author Costin Leau
 * 
 */
public class OsgiServiceDependencyTimedOutEvent extends OsgiServiceDependencyEvent {

	private final long waitingTime;


	/**
	 * Constructs a new <code>OsgiServiceDependencyTimedOutEvent</code>
	 * instance.
	 * 
	 * @param source
	 * @param sourceBeanName
	 * @param dependency
	 */
	public OsgiServiceDependencyTimedOutEvent(Object source, String sourceBeanName, OsgiServiceDependency dependency,
			long waitingTime) {
		super(source, sourceBeanName, dependency);
		this.waitingTime = waitingTime;
	}

	/**
	 * Returns the time (in milliseconds) the source waited for OSGi service to
	 * appear before failing.
	 * 
	 * @return Returns the timeToWait
	 */
	public long getWaitingTime() {
		return waitingTime;
	}
}
