/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.osgi.test;

import java.lang.reflect.Field;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.test.runner.TestRunner;

public class JUnitTestActivatorTest extends TestCase {

	private JUnitTestActivator activator;
//	private ServiceRegistration registration;
//	private ServiceReference reference;

	public static class TestExample implements OsgiJUnitTest {
		private static BundleContext context;

		public void onSetUp() throws Exception {
		}

		public void onTearDown() throws Exception {
		}

		public void osgiRunTest() throws Throwable {
		}

		public void setBundleContext(BundleContext bundleContext) {
			context = bundleContext;
		}

		public BundleContext getBundleContext() {
			return context;
		}

		public void setName(String name) {
		}

	}

	protected void setUp() throws Exception {
		activator = new JUnitTestActivator();
		//reference = new MockServiceReference();
		//registration = new MockServiceRegistration();
	}

	protected void tearDown() throws Exception {

	}

	public void testStart() throws Exception {
		MockControl ctxCtrl = MockControl.createControl(BundleContext.class);
		BundleContext ctx = (BundleContext) ctxCtrl.getMock();

		MockControl servCtrl = MockControl.createControl(TestRunner.class);
		TestRunner runner = (TestRunner) servCtrl.getMock();

		ServiceReference ref = new MockServiceReference();

		ctxCtrl.expectAndReturn(ctx.getServiceReference(TestRunner.class.getName()), ref);
		ctxCtrl.expectAndReturn(ctx.getService(ref), runner);

		ctx.registerService((String) null, null, null);
		ctxCtrl.setMatcher(MockControl.ALWAYS_MATCHER);
		ctxCtrl.setReturnValue(null);

		ctxCtrl.replay();
		servCtrl.replay();

		activator.start(ctx);

		ctxCtrl.verify();
	}

	public void testStop() throws Exception {
		ServiceReference ref = new MockServiceReference();
		MockControl regCtrl = MockControl.createControl(ServiceRegistration.class);
		ServiceRegistration reg = (ServiceRegistration) regCtrl.getMock();
		
		MockControl ctxCtrl = MockControl.createControl(BundleContext.class);
		BundleContext ctx = (BundleContext) ctxCtrl.getMock();
		
		ctxCtrl.expectAndReturn(ctx.ungetService(ref), true);
		reg.unregister();
		
		ctxCtrl.replay();
		regCtrl.replay();
		
		setActivatorField("reference", ref);
		setActivatorField("registration", reg);
		
		activator.stop(ctx);
		
		regCtrl.verify();
		ctxCtrl.verify();
	}

	public void testLoadTest() throws Exception {
		BundleContext ctx = new MockBundleContext();

		MockControl servCtrl = MockControl.createControl(TestRunner.class);
		TestRunner runner = (TestRunner) servCtrl.getMock();

		try {
			activator.executeTest();
			fail("should have thrown exception");
		}
		catch (RuntimeException ex) {
			// expected
		}

		setActivatorField("service", runner);
		runner.runTest(null);
		servCtrl.setMatcher(MockControl.ALWAYS_MATCHER);
		servCtrl.replay();

		setActivatorField("context", ctx);
		System.setProperty(OsgiJUnitTest.OSGI_TEST, TestExample.class.getName());

		activator.executeTest();
		assertSame(ctx, TestExample.context);
		servCtrl.verify();
	}

	private void setActivatorField(String fieldName, Object value) throws Exception {
		Field field = JUnitTestActivator.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(activator, value);

	}
}
