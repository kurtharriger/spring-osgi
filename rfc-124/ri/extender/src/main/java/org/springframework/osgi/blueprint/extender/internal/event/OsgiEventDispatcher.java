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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.blueprint.context.ModuleContextEventConstants;
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
	// match the class inside object class (and use a non backing reference group)
	private static final Pattern PATTERN = Pattern.compile("objectClass=(?:[^\\)]+)");


	public OsgiEventDispatcher(BundleContext bundleContext, PublishType publisher) {
		this.bundleContext = bundleContext;
		this.publisher = publisher;
	}

	public void afterClose(ConfigurableOsgiBundleApplicationContext context) {
		Dictionary<String, Object> props = init(context.getBundle());
		sendEvent(new Event(ModuleContextEventConstants.TOPIC_DESTROYED, props));
	}

	public void afterRefresh(ConfigurableOsgiBundleApplicationContext context) {
		Dictionary<String, Object> props = init(context.getBundle());
		sendEvent(new Event(ModuleContextEventConstants.TOPIC_CREATED, props));
	}

	public void beforeClose(ConfigurableOsgiBundleApplicationContext context) {
		Dictionary<String, Object> props = init(context.getBundle());
		sendEvent(new Event(ModuleContextEventConstants.TOPIC_DESTROYING, props));
	}

	public void beforeRefresh(ConfigurableOsgiBundleApplicationContext context) {
		Dictionary<String, Object> props = init(context.getBundle());
		sendEvent(new Event(ModuleContextEventConstants.TOPIC_CREATING, props));
	}

	public void refreshFailure(ConfigurableOsgiBundleApplicationContext context, Throwable th) {
		Dictionary<String, Object> props = init(context.getBundle());

		props.put(EventConstants.EXCEPTION, th);
		props.put(EventConstants.EXCEPTION_CLASS, th.getClass().getName());
		props.put(EventConstants.EXCEPTION_MESSAGE, th.getMessage());

		sendEvent(new Event(ModuleContextEventConstants.TOPIC_FAILURE, props));
	}

	public void waiting(BootstrappingDependencyEvent event) {
		Dictionary<String, Object> props = init(event.getBundle());
		OsgiServiceDependencyEvent dependencyEvent = event.getDependencyEvent();
		OsgiServiceDependency dependency = dependencyEvent.getServiceDependency();

		Filter filter = dependency.getServiceFilter();
		props.put(EventConstants.EVENT_FILTER, filter.toString());
		// FIXME: there should be a constant for this
		props.put("service.Filter", filter.toString());
		props.put(EventConstants.SERVICE_OBJECTCLASS, extractObjectClassFromFilter(filter));
		sendEvent(new Event(ModuleContextEventConstants.TOPIC_WAITING, props));
	}

	private String[] extractObjectClassFromFilter(Filter filter) {
		if (filter == null) {
			return new String[0];
		}
		List<String> matches = null;
		String filterString = filter.toString();
		Matcher matcher = PATTERN.matcher(filterString);
		while (matcher.find()) {
			if (matches == null) {
				matches = new ArrayList<String>(8);
			}

			matches.add(matcher.group());
		}

		return (matches == null ? new String[0] : matches.toArray(new String[matches.size()]));
	}

	private Dictionary<String, Object> init(Bundle bundle) {
		Dictionary<String, Object> props = new Hashtable<String, Object>();

		// common properties
		props.put(EventConstants.TIMESTAMP, System.currentTimeMillis());

		props.put(EventConstants.BUNDLE, bundle);
		props.put(EventConstants.BUNDLE_ID, bundle.getBundleId());
		// sym name
		props.put(EventConstants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
		props.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
		// version
		Version version = OsgiBundleUtils.getBundleVersion(bundle);
		props.put(Constants.BUNDLE_VERSION, version);
		props.put(ModuleContextEventConstants.BUNDLE_VERSION, version);
		// extender bundle info
		Bundle extenderBundle = bundleContext.getBundle();
		props.put(ModuleContextEventConstants.EXTENDER_BUNDLE, extenderBundle);
		props.put(ModuleContextEventConstants.EXTENDER_ID, extenderBundle.getBundleId());
		props.put(ModuleContextEventConstants.EXTENDER_SYMBOLICNAME, extenderBundle.getSymbolicName());

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