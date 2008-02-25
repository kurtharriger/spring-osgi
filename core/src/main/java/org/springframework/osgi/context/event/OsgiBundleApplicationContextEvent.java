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

package org.springframework.osgi.context.event;

import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;

/**
 * Base class for events raised for an OSGi <code>ApplicationContext</code>.
 * Events of this type are raised by the OSGi extender to notify 3rd parties
 * about changes in the lifecycle of an OSGi application context.
 * 
 * <p/><b>Note:</b>One big difference from
 * <em>traditional</em> <code>ApplicationContextEvent</code> is that it is
 * the extender that sends the notification and not the OSGi application
 * context. This allows third parties to be informed of the life-cycle of an
 * application context without forcing bean initialization inside the source
 * context.
 * 
 * @author Costin Leau
 */
public abstract class OsgiBundleApplicationContextEvent extends ApplicationContextEvent {

	/**
	 * Constructs a new <code>OsgiApplicationContextEvent</code> instance.
	 * 
	 * @param source the <code>ConfigurableOsgiBundleApplicationContext</code>
	 * that the event is raised for (must not be <code>null</code>)
	 */
	public OsgiBundleApplicationContextEvent(ConfigurableOsgiBundleApplicationContext source) {
		super(source);
	}

	/**
	 * Returns the <code>ConfigurableOsgiBundleApplicationContext</code> that
	 * the event was raised for.
	 * 
	 * @return the source of the event as a
	 * <code>ConfigurableOsgiBundleApplicationContext</code> instance.
	 */
	public final ConfigurableOsgiBundleApplicationContext getOsgiBundleApplicationContext() {
		return (ConfigurableOsgiBundleApplicationContext) getSource();
	}

}
