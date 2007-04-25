/*
 * Copyright 2006 the original author or authors.
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
package org.springframework.osgi.extender.support;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.osgi.context.support.ApplicationContextConfiguration; 
import org.springframework.osgi.context.support.EntryLookupControllingMockBundle;
import org.springframework.osgi.context.support.SpringBundleEvent;

public class ApplicationContextCreatorTest extends TestCase {

	private static final String[] META_INF_SPRING_CONTENT = new String[] { "file://META-INF/spring/context.xml",
			"file://META-INF/spring/context-two.xml" };

	private final Map contextMap = new HashMap();

	private final Map initMap = new HashMap();

	private final Map pendingRegistrationTasks = new HashMap();

	private final ApplicationEventMulticaster mcast = new SimpleApplicationEventMulticaster();

	private ApplicationContextCreator createCreator(Bundle aBundle) {
		return new ApplicationContextCreator(aBundle, contextMap, initMap, this.pendingRegistrationTasks, null,
				new ApplicationContextConfiguration(aBundle), mcast) {
			protected ServiceDependentOsgiBundleXmlApplicationContext createApplicationContext(BundleContext context, String[] locations) {
				return new ServiceDependentOsgiBundleXmlApplicationContext(context, locations) {
					public void refresh() throws BeansException {
						// no-op
					}
				};
			}
		};

	}

	public void testNoContextCreatedIfNotSpringPowered() {
		EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(null);
		aBundle.setResultsToReturnOnNextCallToFindEntries(null);
		createCreator(aBundle).run(); // will NPE if not detecting that this
		// bundle is
		// not
		// spring-powered!
	}

	public void testContextIsPlacedIntoPendingMapPriorToRefreshAndMovedAfterwards() {
		EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(null);
		aBundle.setResultsToReturnOnNextCallToFindEntries(META_INF_SPRING_CONTENT);
		final MapTestingBundleXmlApplicationContext testingContext = new MapTestingBundleXmlApplicationContext(
				aBundle.getContext(), META_INF_SPRING_CONTENT);

		ApplicationContextCreator creator = new ApplicationContextCreator(aBundle, contextMap, initMap,
				this.pendingRegistrationTasks, null, new ApplicationContextConfiguration(aBundle), mcast) {

			protected ServiceDependentOsgiBundleXmlApplicationContext createApplicationContext(BundleContext context, String[] locations) {
				return testingContext;
			}
		};
		creator.run();

		assertTrue("context was refreshed", testingContext.isRefreshed);

		Long key = new Long(0);
		assertFalse(initMap.containsKey(key));
		assertTrue(contextMap.containsKey(key));
		assertEquals("context should be in map under bundle id key", testingContext, contextMap.get(key));
	}

	public void testContextIsRemovedFromMapsOnException() {
		EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(null);
		aBundle.setResultsToReturnOnNextCallToFindEntries(META_INF_SPRING_CONTENT);
		final ServiceDependentOsgiBundleXmlApplicationContext testContext = new ServiceDependentOsgiBundleXmlApplicationContext(aBundle.getContext(),
				META_INF_SPRING_CONTENT) {
			public void refresh() {
				throw new RuntimeException("bang! (this exception deliberately caused by test case)") {
					public synchronized Throwable fillInStackTrace() {
						return null;
					}
				};
			}
		};
		ApplicationContextCreator creator = new ApplicationContextCreator(aBundle, contextMap, initMap,
				this.pendingRegistrationTasks, null, new ApplicationContextConfiguration(aBundle), mcast) {

			protected ServiceDependentOsgiBundleXmlApplicationContext createApplicationContext(BundleContext context, String[] locations) {
				return testContext;
			}

		};

		try {
			creator.run();
		}
		catch (Throwable t) {
			fail("Exception should have been handled inside creator");
		}

		Long key = new Long(0);
		assertFalse("failed context not in init map", initMap.containsKey(key));
		assertFalse("failed context not in context map", contextMap.containsKey(key));
	}

	public void testCreationEvent() {
		EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(null);
		aBundle.setResultsToReturnOnNextCallToFindEntries(META_INF_SPRING_CONTENT);
		MockControl mockListener = MockControl.createControl(ApplicationListener.class);
		ApplicationListener listener = (ApplicationListener) mockListener.getMock();
		mcast.addApplicationListener(listener);
		listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STARTING, aBundle));
		listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STARTED, aBundle));
		mockListener.replay();

		createCreator(aBundle).run();

		// mockListener.verify();
		mcast.removeAllListeners();
	}

	public void testCreationFailureEvent() {
		EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(null);
		aBundle.setResultsToReturnOnNextCallToFindEntries(META_INF_SPRING_CONTENT);
		MockControl mockListener = MockControl.createControl(ApplicationListener.class);
		ApplicationListener listener = (ApplicationListener) mockListener.getMock();
		mcast.addApplicationListener(listener);
		final ServiceDependentOsgiBundleXmlApplicationContext testContext = new ServiceDependentOsgiBundleXmlApplicationContext(aBundle.getContext(),
				META_INF_SPRING_CONTENT) {
			public void refresh() {
				throw new RuntimeException("bang! (this exception deliberately caused by test case)") {
					public synchronized Throwable fillInStackTrace() {
						return null;
					}
				};
			}
		};
		ApplicationContextCreator creator = new ApplicationContextCreator(aBundle, contextMap, initMap,
				this.pendingRegistrationTasks, null, new ApplicationContextConfiguration(aBundle), mcast) {

			protected ServiceDependentOsgiBundleXmlApplicationContext createApplicationContext(BundleContext context, String[] locations) {
				return testContext;
			}

		};

		listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STARTING, aBundle));
		listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STOPPED, aBundle));
		mockListener.replay();

		creator.run();

		mockListener.verify();
		mcast.removeAllListeners();
	}

	private class MapTestingBundleXmlApplicationContext extends ServiceDependentOsgiBundleXmlApplicationContext {

		public boolean isRefreshed = false;

		public MapTestingBundleXmlApplicationContext(BundleContext context, String[] configLocations) {
			super(context, configLocations);
		}

		public void refresh() {
			Long key = new Long(0);
			assertTrue("pending map contains this context", initMap.containsKey(key));
			assertEquals("pending map contains this context under bundle id", this, initMap.get(key));
			assertFalse("completed map does not contain this context", contextMap.containsKey(key));
			isRefreshed = true;
		}
	}
}
