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
package org.springframework.osgi.extender;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.context.support.OsgiConfig;
import org.springframework.osgi.extender.support.TestTaskExecutor;
import org.springframework.osgi.mock.EntryLookupControllingMockBundle;
import org.springframework.osgi.mock.MockBundle;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.util.ConfigUtils;

/**
 * @author Adrian Colyer
 * 
 */
public class ContextLoaderListenerTest extends TestCase {

	private ContextLoaderListener listener;

	protected void setUp() throws Exception {
		super.setUp();
		this.listener = new ContextLoaderListener();
	}

	public void testStart() throws Exception {
		MockControl bundleContextControl = MockControl.createControl(BundleContext.class);
		BundleContext context = (BundleContext) bundleContextControl.getMock();
		// platform determination
		context.getProperty(Constants.FRAMEWORK_VENDOR);
		bundleContextControl.setReturnValue(null);

		// extracting bundle id from bundle
		bundleContextControl.expectAndReturn(context.getBundle(), new MockBundle());

		// look for existing resolved bundles
		bundleContextControl.expectAndReturn(context.getBundles(), new Bundle[0]);

		// register namespace and entity resolving service
//		context.registerService((String[]) null, null, null);
//		bundleContextControl.setMatcher(MockControl.ALWAYS_MATCHER);
//		bundleContextControl.setReturnValue(null);

		// register context service
		context.registerService((String[]) null, null, null);
		bundleContextControl.setMatcher(MockControl.ALWAYS_MATCHER);
		bundleContextControl.setReturnValue(null);

		// create task executor
		EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(null);
		aBundle.setEntryReturnOnNextCallToGetEntry(null);
		bundleContextControl.expectAndReturn(context.getBundle(), aBundle);

		// listen for bundle events
		context.addBundleListener(null);
		bundleContextControl.setMatcher(MockControl.ALWAYS_MATCHER);

		bundleContextControl.replay();

		this.listener.start(context);

		bundleContextControl.verify();
	}

	public void tstTaskExecutorCreation() throws Exception {
		MockControl bundleContextControl = MockControl.createNiceControl(BundleContext.class);
		BundleContext context = (BundleContext) bundleContextControl.getMock();
		bundleContextControl.expectAndReturn(context.getBundle(), new MockBundle());
		bundleContextControl.expectAndReturn(context.getBundles(), new Bundle[0]);

		Dictionary headers = new Hashtable();
		headers.put(Constants.BUNDLE_NAME, "Mock Bundle");
		EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(headers);
		aBundle.setEntryReturnOnNextCallToGetEntry(new ClassPathResource("META-INF/spring/moved-extender.xml").getURL());
		bundleContextControl.expectAndReturn(context.getBundle(), aBundle);
		bundleContextControl.expectAndReturn(context.getBundle(), aBundle);
		bundleContextControl.expectAndReturn(context.getBundle(), aBundle);
		bundleContextControl.replay();

		this.listener.start(context);

		MockBundle eventBundle = new MockBundle();
		eventBundle.setBundleId(1);
		BundleEvent event = new BundleEvent(BundleEvent.STARTED, eventBundle);

		try {
			// this.listener.bundleChanged(event);
			fail("Should have thrown exception if taskExecutor was correctly configured");
		}
		catch (RuntimeException ex) {
			assertEquals("test task executor does not really execute!", ex.getMessage());
		}
	}

	public void testTaskExecutor() throws Exception {
		Dictionary headers = new Hashtable();
		headers.put(Constants.BUNDLE_NAME, "Extender mock bundle");
		final EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(headers);
		aBundle.setEntryReturnOnNextCallToGetEntry(new ClassPathResource("META-INF/spring/moved-extender.xml").getURL());

		MockBundleContext ctx = new MockBundleContext() {
			public Bundle getBundle() {
				return aBundle;
			}
		};

		this.listener.start(ctx);

		Dictionary hdrs = new Hashtable();
		hdrs.put(ConfigUtils.SPRING_CONTEXT_HEADER, "bla bla");
		MockBundle anotherBundle = new MockBundle(hdrs);
		anotherBundle.setBundleId(1);
		
		BundleEvent event = new BundleEvent(BundleEvent.STARTED, anotherBundle);

		BundleListener listener = (BundleListener) ctx.getBundleListeners().iterator().next();

		TestTaskExecutor.called = false;
		
		listener.bundleChanged(event);
		assertTrue("task executor should have been called if configured properly", TestTaskExecutor.called);
	}

}
