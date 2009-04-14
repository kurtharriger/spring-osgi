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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.context.ModuleContextListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.springframework.core.io.Resource;
import org.springframework.osgi.rfc124.iandt.BaseRFC124IntegrationTest;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Check whether events are raised at the right time.
 * 
 * @author Costin Leau
 */
public class EventTest extends BaseRFC124IntegrationTest {

	public void testFailingBundleEvents() throws Exception {

		final List<Bundle> startedBundles = new ArrayList<Bundle>();
		final Map<Bundle, Throwable> failedBundles = new LinkedHashMap<Bundle, Throwable>();

		ModuleContextListener listener = new ModuleContextListener() {

			public void contextCreated(Bundle bundle) {
				startedBundles.add(bundle);
			}

			public void contextCreationFailed(Bundle bundle, Throwable throwable) {
				failedBundles.put(bundle, throwable);
			}
		};

		// register Blueprint registration
		ServiceRegistration reg = bundleContext.registerService(ModuleContextListener.class.getName(), listener, null);

		EventHandler handler = new EventHandler() {

			public void handleEvent(Event event) {
				StringBuilder builder = new StringBuilder();
				builder.append("Received event ").append(event).append(" w/ properties \n");

				String[] propNames = event.getPropertyNames();

				for (String propName : propNames) {
					builder.append(propName).append("=").append(event.getProperty(propName)).append("\n");
				}

				System.out.println(builder.toString());
			}
		};

		String[] topics = new String[] { "org/osgi/service/*" };
		Dictionary<String, Object> prop = new Hashtable<String, Object>();
		prop.put(EventConstants.EVENT_TOPIC, topics);
		bundleContext.registerService(EventHandler.class.getName(), handler, prop);

		System.out.println("Installed Bundles " + Arrays.toString(bundleContext.getBundles()));
		Resource bundleResource = getLocator().locateArtifact("org.springframework.osgi.rfc124.iandt", "error.bundle",
			getSpringDMVersion());
		Bundle failingBundle = bundleContext.installBundle(bundleResource.getDescription(),
			bundleResource.getInputStream());

		// start bundle
		failingBundle.start();
		Thread.sleep(1000 * 3);

		reg.unregister();

		assertEquals(1, failedBundles.size());
		assertEquals(failingBundle, failedBundles.keySet().iterator().next());
	}
}