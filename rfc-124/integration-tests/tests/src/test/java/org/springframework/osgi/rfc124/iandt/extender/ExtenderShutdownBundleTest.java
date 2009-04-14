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

package org.springframework.osgi.rfc124.iandt.extender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.springframework.osgi.rfc124.iandt.BaseRFC124IntegrationTest;
import org.springframework.osgi.util.OsgiBundleUtils;

/**
 * Test that checkes whether the proper events are sent even when the extender
 * bundle is being destroyed.
 * 
 * @author Costin Leau
 */
public class ExtenderShutdownBundleTest extends BaseRFC124IntegrationTest {

	@Override
	protected String[] getTestBundlesNames() {
		return new String[] { "org.springframework.osgi.rfc124.iandt, simple.bundle," + getSpringDMVersion() };
	}

	public void testExtenderShutdown() throws Exception {
		final List<Event> events = Collections.synchronizedList(new ArrayList<Event>());
		Bundle extenderBundle = OsgiBundleUtils.findBundleBySymbolicName(bundleContext,
			"org.osgi.service.blueprint.all");
		assertNotNull(extenderBundle);

		EventHandler handler = new EventHandler() {

			public void handleEvent(Event event) {
				StringBuilder builder = new StringBuilder();
				builder.append("Received event ").append(event).append(" w/ properties \n");

				String[] propNames = event.getPropertyNames();

				for (String propName : propNames) {
					builder.append(propName).append("=").append(event.getProperty(propName)).append("\n");
				}
				events.add(event);
			}
		};

		String[] topics = new String[] { "org/osgi/service/*" };
		Dictionary<String, Object> prop = new Hashtable<String, Object>();
		prop.put(EventConstants.EVENT_TOPIC, topics);
		bundleContext.registerService(EventHandler.class.getName(), handler, prop);

		System.out.println("About to stop the extender..");

		extenderBundle.stop();

		assertFalse("no event received", events.isEmpty());
	}
}