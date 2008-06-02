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

import org.springframework.osgi.service.importer.OsgiServiceDependency;

/**
 * @author Costin Leau
 * 
 */
// FIXME: change name - this is a waiting successeded event rather then a generic satisfied event
public class OsgiServiceDependencySatisfiedEvent extends OsgiServiceDependencyEvent {

	private static final long serialVersionUID = 4798850518871798024L;

	private long waitedTime;


	/**
	 * Constructs a new <code>OsgiServiceDependencySatisfiedEvent</code>
	 * instance.
	 * 
	 * @param source
	 * @param sourceBeanName
	 * @param dependency
	 */
	public OsgiServiceDependencySatisfiedEvent(Object source, OsgiServiceDependency dependency, long waitedTime) {
		super(source, dependency);
		this.waitedTime = waitedTime;
	}

	/**
	 * Returns the time spent waiting before the service was found (and the
	 * dependency considered satisfied)
	 * 
	 * @return Returns the waitedTime
	 */
	public long getWaitedTime() {
		return waitedTime;
	}
}
