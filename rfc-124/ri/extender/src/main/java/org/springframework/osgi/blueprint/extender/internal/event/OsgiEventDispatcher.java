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

package org.springframework.osgi.blueprint.extender.internal.event;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.service.importer.OsgiServiceDependency;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.osgi.util.OsgiBundleUtils;

/**
 * Actual {@link EventAdmin} dispatcher. Implemented as a package-protected
 * generic class that can be further configured by the user-facing classes. The
 * additional benefit is that being a separate class, this class can be used
 * only when the org.osgi.service.event package is available, w/o triggering
 * runtime class loading.
 * 
 * @author Costin Leau
 * 
 */
class OsgiEventDispatcher implements EventDispatcher {

	private final BundleContext bundleContext;
	private final PublishType publisher;

	// constants
	private static final String ROOT_TOPIC = "org/osgi/service/blueprint/context/";
	private static final String CREATING_TOPIC = ROOT_TOPIC + "CREATING";
	private static final String CREATED_TOPIC = ROOT_TOPIC + "CREATED";
	private static final String DESTROYING_TOPIC = ROOT_TOPIC + "DESTROYING";
	private static final String DESTROYED_TOPIC = ROOT_TOPIC + "DESTROYED";
	private static final String WAITING_TOPIC = ROOT_TOPIC + "WAITING";
	private static final String FAILURE_TOPIC = ROOT_TOPIC + "FAILURE";


	public OsgiEventDispatcher(BundleContext bundleContext, PublishType publisher) {
		this.bundleContext = bundleContext;
		this.publisher = publisher;
	}

	public void afterClose(ConfigurableOsgiBundleApplicationContext context) {
		Dictionary<String, Object> props = init(context.getBundle());
		sendEvent(new Event(DESTROYED_TOPIC, props));
	}

	public void afterRefresh(ConfigurableOsgiBundleApplicationContext context) {
		Dictionary<String, Object> props = init(context.getBundle());
		sendEvent(new Event(CREATED_TOPIC, props));
	}

	public void beforeClose(ConfigurableOsgiBundleApplicationContext context) {
		Dictionary<String, Object> props = init(context.getBundle());
		sendEvent(new Event(DESTROYING_TOPIC, props));
	}

	public void beforeRefresh(ConfigurableOsgiBundleApplicationContext context) {
		Dictionary<String, Object> props = init(context.getBundle());
		sendEvent(new Event(CREATING_TOPIC, props));
	}

	public void refreshFailure(ConfigurableOsgiBundleApplicationContext context, Throwable th) {
		Dictionary<String, Object> props = init(context.getBundle());

		props.put(EventConstants.EXCEPTION, th);
		props.put(EventConstants.EXCEPTION_CLASS, th.getClass().getName());
		props.put(EventConstants.EXCEPTION_MESSAGE, th.getMessage());

		sendEvent(new Event(FAILURE_TOPIC, props));
	}

	public void waiting(BootstrappingDependencyEvent event) {
		Dictionary<String, Object> props = init(event.getBundle());
		OsgiServiceDependencyEvent dependencyEvent = event.getDependencyEvent();
		OsgiServiceDependency dependency = dependencyEvent.getServiceDependency();

		Filter filter = dependency.getServiceFilter();
		// FIXME: the spec refers to a SERVICE_FILTER property 
		props.put(EventConstants.EVENT_FILTER, filter.toString());
		// FIXME: idem, prop SERVICE_CLASS doesn't exist
		// moreover the filter already contains this info
		// props.put(EventConstants.SERVICE_OBJECTCLASS, "FIXME");
		sendEvent(new Event(WAITING_TOPIC, props));
	}

	private Dictionary<String, Object> init(Bundle bundle) {
		Dictionary<String, Object> props = new Hashtable<String, Object>();

		// common properties
		props.put(EventConstants.TIMESTAMP, System.currentTimeMillis());

		props.put(EventConstants.BUNDLE, bundle);
		props.put(EventConstants.BUNDLE_ID, bundle.getBundleId());
		props.put(EventConstants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());

		props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
		// FIXME: the spec refers to a BUNDLE_VERSION prop - is this the one?
		props.put(Constants.BUNDLE_VERSION, OsgiBundleUtils.getBundleVersion(bundle));

		return props;
	}

	private void sendEvent(Event osgiEvent) {
		if (osgiEvent != null) {
			ServiceReference ref = bundleContext.getServiceReference(EventAdmin.class.getName());
			if (ref != null) {
				EventAdmin eventAdmin = (EventAdmin) bundleContext.getService(ref);
				if (eventAdmin != null) {
					publisher.publish(eventAdmin, osgiEvent);
				}
			}
		}
	}
}
