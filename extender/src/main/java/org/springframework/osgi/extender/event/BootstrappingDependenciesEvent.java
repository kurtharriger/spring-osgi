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
package org.springframework.osgi.extender.event;

import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.springframework.context.ApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.util.Assert;

/**
 * Spring-DM Extender bootstrapping event. Used during the application context discovery phase, before an application
 * context is fully initialized. Similar to {@link BootstrappingDependencyEvent}, this event contains the information
 * regarding all unsatisfied dependencies.
 * 
 * Consider using this event {@link BootstrappingDependencyEvent} for getting a global overview of the waiting
 * application and {@link BootstrappingDependencyEvent} for finding out specific information.
 * 
 * <p/> It can be used to receive status updates for contexts started by the extender.
 * 
 * @author Costin Leau
 */
public class BootstrappingDependenciesEvent extends OsgiBundleApplicationContextEvent {

	private final Collection<OsgiServiceDependencyEvent> dependencyEvents;
	private final Filter dependenciesFilter;
	private final long timeLeft;

	/**
	 * Constructs a new <code>BootstrappingDependencyEvent</code> instance.
	 * 
	 * @param source
	 */
	public BootstrappingDependenciesEvent(ApplicationContext source, Bundle bundle,
			Collection<OsgiServiceDependencyEvent> nestedEvents, Filter filter, long timeLeft) {
		super(source, bundle);
		Assert.notNull(nestedEvents);
		this.dependencyEvents = nestedEvents;
		this.dependenciesFilter = filter;
		this.timeLeft = timeLeft;
	}

	/**
	 * Returns the nested, dependency event that caused the bootstrapping event to be raised.
	 * 
	 * @return associated dependency event
	 */
	public Collection<OsgiServiceDependencyEvent> getDependencyEvents() {
		return dependencyEvents;
	}

	public Filter getDependenciesAsFilter() {
		return dependenciesFilter;
	}

	public long getTimeToWait() {
		return timeLeft;
	}
}