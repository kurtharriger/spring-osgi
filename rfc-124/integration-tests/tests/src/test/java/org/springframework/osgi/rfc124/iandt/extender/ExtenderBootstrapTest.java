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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.Version;
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.ModuleContextListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.springframework.core.io.Resource;
import org.springframework.osgi.rfc124.iandt.BaseRFC124IntegrationTest;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Integration test that checks basic behaviour signs of the RFC 124 Extender.
 * 
 * @author Costin Leau
 * 
 */
public class ExtenderBootstrapTest extends BaseRFC124IntegrationTest {

	private Bundle testBundle;
	private final Object monitor = new Object();


	@Override
	protected void onSetUp() throws Exception {
		super.onSetUp();
		installTestBundle();
	}

	@Override
	protected void onTearDown() throws Exception {
		if (testBundle != null) {
			testBundle.uninstall();
		}
	}

	public void testSanity() throws Exception {

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

		testBundle.start();
		Thread.sleep(1000 * 3);
		testBundle.stop();
		Thread.sleep(1000 * 3);
	}

	public void tstModuleContextService() throws Exception {
		installTestBundle();
		final boolean[] receviedEvent = new boolean[1];
		ServiceListener notifier = new ServiceListener() {

			public void serviceChanged(ServiceEvent event) {
				//ModuleContext.SYMBOLIC_NAME_PROPERTY was removed - need to find a replacement
				if (event.getServiceReference().getProperty(Constants.BUNDLE_SYMBOLICNAME) != null) {
					logger.info("Found service "
							+ OsgiServiceReferenceUtils.getServicePropertiesSnapshotAsMap(event.getServiceReference()));

					synchronized (monitor) {
						receviedEvent[0] = true;
						monitor.notify();
					}
				}
			}
		};
		// wait for the service up to 3 minutes
		long waitTime = 2 * 60 * 1000;
		bundleContext.addServiceListener(notifier);

		testBundle.start();

		synchronized (monitor) {
			monitor.wait(waitTime);
			assertTrue(receviedEvent[0]);
		}
		bundleContext.removeServiceListener(notifier);

		testBundle.stop();
		assertNull("module context service should be unpublished",
			bundleContext.getServiceReference(ModuleContext.class.getName()));
	}

	public void testModuleContextListener() throws Exception {
		final List<Bundle> contexts = new ArrayList<Bundle>();
		ModuleContextListener listener = new ModuleContextListener() {

			public void contextCreated(Bundle bundle) {
				addToList(bundle);
			}

			public void contextCreationFailed(Bundle bundle, Throwable ex) {
				addToList(bundle);
			}

			private void addToList(Bundle bundle) {
				synchronized (contexts) {
					contexts.add(bundle);
					contexts.notify();
				}
			}
		};

		installTestBundle();
		bundleContext.registerService(ModuleContextListener.class.getName(), listener, null);

		testBundle.start();
		synchronized (contexts) {
			contexts.wait(2 * 1000 * 60);
			assertFalse("no event received", contexts.isEmpty());
		}
	}

	public void testLogInstalledBundles() throws Exception {
		for (Bundle bundle : bundleContext.getBundles()) {
			System.out.println("[" + bundle.getBundleId() + "] " + OsgiStringUtils.bundleStateAsString(bundle) + " "
					+ OsgiStringUtils.nullSafeName(bundle) + "|" + bundle.getSymbolicName() + " @ "
					+ bundle.getLocation());
		}
	}

	private void installTestBundle() throws Exception {
		Resource bundleResource = getLocator().locateArtifact("org.springframework.osgi.rfc124.iandt", "simple.bundle",
			getSpringDMVersion());
		testBundle = bundleContext.installBundle(bundleResource.getDescription(), bundleResource.getInputStream());
	}
}