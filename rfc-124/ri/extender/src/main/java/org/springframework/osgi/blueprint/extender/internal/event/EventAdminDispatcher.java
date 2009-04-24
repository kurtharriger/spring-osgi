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

package org.springframework.osgi.blueprint.extender.internal.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.util.ClassUtils;

/**
 * Dispatcher that transforms Spring application context lifecycle events into
 * notifications to the OSGi EventAdmin service.
 * 
 * <b>Note:</b> This class does not assume the EventAdmin service or classes
 * are available. If the classes are missing, the dispatcher will not publish
 * any events during its life time. If the service is unavailable, the
 * dispatcher will stop sending events until the service becomes available.
 * 
 * @author Costin Leau
 */
public class EventAdminDispatcher implements OsgiBundleApplicationContextListener {

	/** logger */
	private static final Log log;

	/** Whether the Event Admin library is present on the classpath */
	private static final boolean eventAdminAvailable;

	static {
		eventAdminAvailable = ClassUtils.isPresent("org.osgi.service.event.EventAdmin",
			EventAdminDispatcher.class.getClassLoader());

		log = LogFactory.getLog(EventAdminDispatcher.class);

		if (!eventAdminAvailable) {
			log.info("EventAdmin package not found; no Module Lifecycle Events will be published");
		}
	}


	/**
	 * Actual creation of EventAdmin dispatcher. In separate inner class to
	 * avoid runtime dependency on EventAdmin classes.
	 * 
	 * @author Costin Leau
	 */
	private static abstract class EventAdminDispatcherFactory {

		private static EventDispatcher createDispatcher(BundleContext bundleContext) {
			return new OsgiEventDispatcher(bundleContext, PublishType.POST);
		}
	}


	/** actual dispatcher */
	private final EventDispatcher dispatcher;


	public EventAdminDispatcher(BundleContext bundleContext) {
		boolean trace = log.isTraceEnabled();
		if (eventAdminAvailable) {
			if (trace)
				log.trace("Creating [org.springframework.osgi.blueprint.context.event.OsgiEventDispatcher]");
			dispatcher = EventAdminDispatcherFactory.createDispatcher(bundleContext);
		}
		else {
			dispatcher = null;
		}
	}

	public void beforeClose(ConfigurableOsgiBundleApplicationContext context) {
		if (dispatcher != null) {
			dispatcher.beforeClose(context);
		}
	}

	public void beforeRefresh(ConfigurableOsgiBundleApplicationContext context) {
		if (dispatcher != null) {
			dispatcher.beforeRefresh(context);
		}
	}

	public void afterClose(ConfigurableOsgiBundleApplicationContext context) {
		if (dispatcher != null) {
			dispatcher.afterClose(context);
		}
	}

	public void afterRefresh(ConfigurableOsgiBundleApplicationContext context) {
		if (dispatcher != null) {
			dispatcher.afterRefresh(context);
		}
	}

	public void refreshFailure(ConfigurableOsgiBundleApplicationContext context, Throwable th) {
		if (dispatcher != null) {
			dispatcher.refreshFailure(context, th);
		}
	}

	public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
		if (dispatcher != null) {
			if (event instanceof BootstrappingDependencyEvent) {
				dispatcher.waiting((BootstrappingDependencyEvent) event);
			}
		}
	}
}
