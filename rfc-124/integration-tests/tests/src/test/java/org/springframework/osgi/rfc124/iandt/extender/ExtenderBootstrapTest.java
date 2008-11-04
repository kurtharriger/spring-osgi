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

package org.springframework.osgi.rfc124.iandt.extender;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.springframework.core.io.Resource;
import org.springframework.osgi.rfc124.iandt.BaseRFC124IntegrationTest;

/**
 * Simple test for that boostraps the RFC124 extender.
 * 
 * @author Costin Leau
 * 
 */
public class ExtenderBootstrapTest extends BaseRFC124IntegrationTest {

	public void testSanity() throws Exception {
		Resource res = getLocator().locateArtifact("org.springframework.osgi.rfc124.iandt", "simple.bundle",
			getSpringDMVersion());

		final Bundle simpleBundle = bundleContext.installBundle(res.getDescription(), res.getInputStream());

		EventHandler handler = new EventHandler() {

			public void handleEvent(Event event) {
				System.out.println("Received event " + event);
			}
		};

		String[] topics = new String[] { "org/osgi/service/*" };
		Dictionary<String, Object> prop = new Hashtable<String, Object>();
		prop.put(EventConstants.EVENT_TOPIC, topics);
		bundleContext.registerService(EventHandler.class.getName(), handler, prop);

		simpleBundle.start();
		Thread.sleep(1000 * 3);
		simpleBundle.stop();
		Thread.sleep(1000 * 3);
	}
}
