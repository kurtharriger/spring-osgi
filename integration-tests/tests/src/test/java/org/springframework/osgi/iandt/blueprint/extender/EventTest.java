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

package org.springframework.osgi.iandt.blueprint.extender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.springframework.core.io.Resource;
import org.springframework.osgi.iandt.blueprint.BaseBlueprintIntegrationTest;

/**
 * Check whether events are raised at the right time.
 * 
 * @author Costin Leau
 */
public class EventTest extends BaseBlueprintIntegrationTest {

	final List<Bundle> startedBundles = new ArrayList<Bundle>();
	final Map<Bundle, Throwable> failedBundles = new LinkedHashMap<Bundle, Throwable>();
	private ServiceRegistration reg1, reg2;

	BlueprintListener listener = new BlueprintListener() {

		public void blueprintEvent(BlueprintEvent event) {
			switch (event.getType()) {
			case BlueprintEvent.CREATED:
				startedBundles.add(event.getBundle());
				break;

			case BlueprintEvent.FAILURE:
				failedBundles.put(event.getBundle(), event.getCause());
				break;

			default:
				System.out.println("Received event " + event);
			}
		}
	};

	EventHandler handler = new EventHandler() {

		public void handleEvent(Event event) {
			StringBuilder builder = new StringBuilder();
			builder.append("Received event ").append(event).append(" w/ properties \n");

			String[] propNames = event.getPropertyNames();

			for (String propName : propNames) {
				builder.append(propName).append("=").append(event.getProperty(propName)).append("\n");
			}

			System.out.println(builder.toString());
			if (event.getTopic().startsWith("org/osgi/service/blueprint/container/FAILURE")) {
				failedBundles.put((Bundle) event.getProperty("bundle"), (Throwable) event.getProperty("exception"));
			}
		}
	};

	@Override
	protected void onSetUp() throws Exception {
		startedBundles.clear();
		failedBundles.clear();

		// register Blueprint registration
		reg1 = bundleContext.registerService(BlueprintListener.class.getName(), listener, null);

		// register EventAdmin
		String[] topics = new String[] { "org/osgi/service/*" };
		Dictionary<String, Object> prop = new Hashtable<String, Object>();
		prop.put(EventConstants.EVENT_TOPIC, topics);
		reg2 = bundleContext.registerService(EventHandler.class.getName(), handler, prop);

	}

	@Override
	protected void onTearDown() throws Exception {
		startedBundles.clear();
		failedBundles.clear();

		reg1.unregister();
		reg2.unregister();
	}

	public void testFailingBundleEvents() throws Exception {
		System.out.println("Installed Bundles " + Arrays.toString(bundleContext.getBundles()));
		Resource bundleResource =
				getLocator().locateArtifact("org.springframework.osgi.iandt.blueprint", "error.bundle",
						getSpringDMVersion());
		Bundle failingBundle =
				bundleContext.installBundle(bundleResource.getDescription(), bundleResource.getInputStream());

		// start bundle
		failingBundle.start();
		Thread.sleep(1000 * 3);

		assertEquals(1, failedBundles.size());
		assertEquals(failingBundle, failedBundles.keySet().iterator().next());
	}

	public void testFailureOnDependenciesEvent() throws Exception {
		System.out.println("Installed Bundles " + Arrays.toString(bundleContext.getBundles()));
		Resource bundleResource =
				getLocator().locateArtifact("org.springframework.osgi.iandt.blueprint", "waiting.bundle",
						getSpringDMVersion());
		Bundle failingBundle =
				bundleContext.installBundle(bundleResource.getDescription(), bundleResource.getInputStream());

		failingBundle.start();
		Thread.sleep(1000 * 5);
		assertEquals(1, failedBundles.size());
		System.out.println("Failed bundles are " + failedBundles.values());
	}
}