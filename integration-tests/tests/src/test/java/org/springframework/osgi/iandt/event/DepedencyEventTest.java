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
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependency;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencySatisfiedEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitingEvent;

/**
 * @author Costin Leau
 * 
 */
public class DepedencyEventTest extends AbstractEventTest {

	protected String[] getTestBundlesNames() {
		// no bundle for starters
		return new String[] {};
	}

	protected void onSetUp() throws Exception {
		super.onSetUp();

		// override the listener with another implementation that waits until the appCtx are fully started
		listener = new OsgiBundleApplicationContextListener() {

			public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
				System.out.println("receiving event " + event.getClass());
				eventList.add(event);
				synchronized (lock) {
					lock.notify();
				}
			}
		};

	}

	public void testEventsForCtxThatWork() throws Exception {
		// publish listener
		registerEventListener();

		assertTrue("should start with an empty list", eventList.isEmpty());

		// install the dependency bundle
		Resource bundle = getLocator().locateArtifact("org.springframework.osgi.iandt", "dependencies",
			getSpringDMVersion());

		Resource dependency1 = getLocator().locateArtifact("org.springframework.osgi.iandt", "simple.service",
			getSpringDMVersion());

		Resource dependency2 = getLocator().locateArtifact("org.springframework.osgi.iandt", "simple.service2",
			getSpringDMVersion());

		Resource dependency3 = getLocator().locateArtifact("org.springframework.osgi.iandt", "simple.service3",
			getSpringDMVersion());

		Bundle bnd = bundleContext.installBundle(bundle.getURL().toExternalForm());

		// install the bundles but don't start them
		Bundle bnd1 = bundleContext.installBundle(dependency1.getURL().toExternalForm());
		Bundle bnd2 = bundleContext.installBundle(dependency2.getURL().toExternalForm());
		Bundle bnd3 = bundleContext.installBundle(dependency3.getURL().toExternalForm());

		try {

			bnd.start();

			// wait 2 seconds to make sure the events properly propagate
			Thread.sleep(2 * 1000);

			// then check the event queue
			System.out.println(eventList.size());
			System.out.println(eventList);

			// expect 3 events at least (3 importers unsatisfied)
			assertTrue(eventList.size() >= 3);

			// check the event type and their name (plus the order)

			// simple service 3
			assertEquals("&simpleService3", getDependencyAt(0).getBeanName());
			assertEquals(OsgiServiceDependencyWaitingEvent.class, getNestedEventAt(0).getClass());
			// simple service 2
			assertEquals("&simpleService2", getDependencyAt(1).getBeanName());
			assertEquals(OsgiServiceDependencyWaitingEvent.class, getNestedEventAt(0).getClass());
			// simple service 1
			assertEquals("&nested", getDependencyAt(2).getBeanName());
			assertEquals(OsgiServiceDependencyWaitingEvent.class, getNestedEventAt(0).getClass());

			// start the dependencies first dependency
			bnd1.start();
			// make sure it's fully started
			waitOnContextCreation("org.springframework.osgi.iandt.simpleservice");
			assertEquals("&nested", getDependencyAt(3).getBeanName());
			assertEquals(OsgiServiceDependencySatisfiedEvent.class, getNestedEventAt(3).getClass());

			bnd3.start();
			// make sure it's fully started
			waitOnContextCreation("org.springframework.osgi.iandt.simpleservice3");
			assertEquals("&simpleService3", getDependencyAt(5).getBeanName());
			assertEquals(OsgiServiceDependencySatisfiedEvent.class, getNestedEventAt(5).getClass());
			// bnd3 context started event

			bnd2.start();
			waitOnContextCreation("org.springframework.osgi.iandt.simpleservice2");
			assertEquals("&simpleService2", getDependencyAt(7).getBeanName());
			assertEquals(OsgiServiceDependencySatisfiedEvent.class, getNestedEventAt(7).getClass());
			// bnd2 context started event
			// wait until the bundle fully starts
			waitOnContextCreation("org.springframework.osgi.iandt.dependencies");
			// double check context started event

			// bnd1 context started event

			//			assertEquals(OsgiBundleContextRefreshedEvent.class, eventList.get(4).getClass());
			//			assertEquals(OsgiBundleContextRefreshedEvent.class, eventList.get(6).getClass());
			//			assertEquals(OsgiBundleContextRefreshedEvent.class, eventList.get(8).getClass());
			//			assertEquals(OsgiBundleContextRefreshedEvent.class, eventList.get(9).getClass());

		}
		finally {
			bnd.uninstall();

			bnd1.uninstall();
			bnd2.uninstall();
			bnd3.uninstall();
		}
	}

	private OsgiServiceDependency getDependencyAt(int index) {
		return getNestedEventAt(index).getServiceDependency();
	}

	private OsgiServiceDependencyEvent getNestedEventAt(int index) {
		Object obj = eventList.get(index);
		System.out.println("received object " + obj.getClass() + "|" + obj);
		BootstrappingDependencyEvent event = (BootstrappingDependencyEvent) obj;
		return event.getDependencyEvent();
	}
}
