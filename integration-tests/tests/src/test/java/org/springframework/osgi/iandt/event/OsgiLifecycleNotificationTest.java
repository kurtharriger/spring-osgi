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

package org.springframework.osgi.iandt.event;

import org.osgi.framework.Bundle;
import org.springframework.core.io.Resource;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;

/**
 * Integration test for the appCtx notification mechanism.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiLifecycleNotificationTest extends AbstractEventTest {

	protected String[] getTestBundlesNames() {
		return new String[] { "org.springframework.osgi.iandt, extender.listener.bundle," + getSpringDMVersion() };
	}

	protected void onSetUp() throws Exception {
		super.onSetUp();
		listener = new OsgiBundleApplicationContextListener() {

			public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
				if (event instanceof OsgiBundleContextRefreshedEvent || event instanceof OsgiBundleContextFailedEvent) {
					eventList.add(event);
					synchronized (lock) {
						lock.notify();
					}
				}
			}
		};
	}

	public void testEventsForCtxThatWork() throws Exception {
		registerEventListener();

		assertTrue("should start with an empty list", eventList.isEmpty());
		// install a simple osgi bundle and check the list of events

		Resource bundle = getLocator().locateArtifact("org.springframework.osgi.iandt", "simple.service",
			getSpringDMVersion());

		Bundle bnd = bundleContext.installBundle(bundle.getURL().toExternalForm());
		try {

			bnd.start();

			assertTrue("event not received", waitForEvent(TIME_OUT));
			System.out.println("event received " + eventList);
			assertEquals("wrong number of events received", 1, eventList.size());
			Object event = eventList.get(0);
			assertTrue("wrong event received", event instanceof OsgiBundleContextRefreshedEvent);
		}
		finally {
			bnd.uninstall();
		}
	}

	public void testEventsForCtxThatFail() throws Exception {
		registerEventListener();

		assertTrue("should start with an empty list", eventList.isEmpty());
		// install a simple osgi bundle and check the list of events

		Resource bundle = getLocator().locateArtifact("org.springframework.osgi.iandt", "error", getSpringDMVersion());

		Bundle bnd = bundleContext.installBundle(bundle.getURL().toExternalForm());

		try {
			bnd.start();

			assertTrue("event not received", waitForEvent(TIME_OUT));
			System.out.println("event received " + eventList);
			assertEquals("wrong number of events received", 1, eventList.size());
			Object event = eventList.get(0);
			assertTrue("wrong event received", event instanceof OsgiBundleContextFailedEvent);
		}
		finally {
			bnd.uninstall();
		}
	}
}
