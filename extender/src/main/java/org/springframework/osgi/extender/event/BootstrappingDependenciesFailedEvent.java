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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.springframework.context.ApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;

/**
 * Bootstrapping event indicating a context has failed to initializsed due to unsatisfied mandatory dependencies.
 * 
 * @author Costin Leau
 */
public class BootstrappingDependenciesFailedEvent extends OsgiBundleContextFailedEvent {

	private final Collection<OsgiServiceDependencyEvent> dependencyEvents;
	private final Collection<String> dependencyFilters;
	private final Filter dependenciesFilter;

	public BootstrappingDependenciesFailedEvent(ApplicationContext source, Bundle bundle, Throwable th,
			Collection<OsgiServiceDependencyEvent> nestedEvents, Filter filter) {
		super(source, bundle, th);

		this.dependencyEvents = nestedEvents;
		this.dependenciesFilter = filter;

		List<String> depFilters = new ArrayList<String>(dependencyEvents.size());

		for (OsgiServiceDependencyEvent dependency : nestedEvents) {
			depFilters.add(dependency.getServiceDependency().getServiceFilter().toString());
		}

		dependencyFilters = Collections.unmodifiableCollection(depFilters);
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

	public Collection<String> getDependencyFilters() {
		return dependencyFilters;
	}
}
