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

package org.springframework.osgi.extender.internal.activator;

import java.util.Iterator;
import java.util.List;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;

/**
 * Listener dispatching OSGi events to interested listeners.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiListenerWrapper implements ApplicationListener {

	private final List osgiListeners;


	public OsgiListenerWrapper(List listeners) {
		this.osgiListeners = listeners;
	}

	// filter non-osgi events
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof OsgiBundleApplicationContextEvent) {
			OsgiBundleApplicationContextEvent osgiEvent = (OsgiBundleApplicationContextEvent) event;
			for (Iterator iterator = osgiListeners.iterator(); iterator.hasNext();) {
				OsgiBundleApplicationContextListener listener = (OsgiBundleApplicationContextListener) iterator.next();
				listener.onOsgiApplicationEvent(osgiEvent);
			}
		}
	}
}
