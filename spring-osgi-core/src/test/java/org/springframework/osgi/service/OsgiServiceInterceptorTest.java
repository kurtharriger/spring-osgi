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
 *
 * Created on 26-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.easymock.internal.AlwaysMatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.target.HotSwappableTargetSource;

/**
 * @author Adrian Colyer
 * @author Hal Hildebrand
 * @since 2.0
 */
public class OsgiServiceInterceptorTest extends TestCase {

	private OsgiServiceInterceptor interceptor;
	private MockControl mockContextControl;
	private BundleContext bundleContext;
	private ServiceReference serviceRef;
	private HotSwappableTargetSource tgtSource;
	private SI targetObject;


	protected void setUp() throws Exception {
		super.setUp();
		this.mockContextControl = MockControl.createControl(BundleContext.class);
		this.bundleContext = (BundleContext) this.mockContextControl.getMock();
		this.serviceRef = getServiceReference();
		this.targetObject = new SI() {
			public void process() {}
		};
		this.tgtSource = new HotSwappableTargetSource(targetObject);
	}

	/**
	 * 
	 */
	private void createInterceptor() {
		this.interceptor = new OsgiServiceInterceptor(
			this.tgtSource,
			SI.class,
			new Object()
		);

	}

	public void tstServiceModified() {
		this.bundleContext.addServiceListener(null);
		this.mockContextControl.setMatcher(new AlwaysMatcher());
		this.bundleContext.getService(this.serviceRef);
		SI newTarget = new SI() {
			public void process() {}
		};
		this.mockContextControl.setReturnValue(newTarget);
		this.mockContextControl.replay();

		createInterceptor();
		assertSame("should have original target", this.targetObject,
			this.tgtSource.getTarget());

		this.mockContextControl.verify();
		assertSame("target has been swapped", newTarget, this.tgtSource.getTarget());
	}

	public void tstServiceUnregisteredAndRebinds() throws Throwable {
		this.bundleContext.addServiceListener(null);
		this.mockContextControl.setMatcher(new AlwaysMatcher());
		this.bundleContext.getServiceReferences(SI.class.getName(), "(attr=value)");
		this.mockContextControl.setReturnValue(new ServiceReference[0]);
		this.bundleContext.getAllServiceReferences(SI.class.getName(), "(attr=value)");
		this.mockContextControl.setReturnValue(new ServiceReference[0]);
		this.bundleContext.getServiceReferences(SI.class.getName(), "(attr=value)");
		ServiceReference newServiceReference = getServiceReference();
		this.mockContextControl.setReturnValue(new ServiceReference[]{newServiceReference});
		this.bundleContext.getService(newServiceReference);
		SI newTarget = new SI() {
			public void process() {}
		};
		this.mockContextControl.setReturnValue(newTarget);
		this.mockContextControl.replay();

		createInterceptor();
		assertSame("should have original target", this.targetObject,
			this.tgtSource.getTarget());
		this.interceptor.before(null, null, null);

		this.mockContextControl.verify();
		assertSame("target has been swapped", newTarget, this.tgtSource.getTarget());
	}

	public void tstServiceUnregisteredAndFailsToRebind() throws Throwable {
		this.bundleContext.addServiceListener(null);
		this.mockContextControl.setMatcher(new AlwaysMatcher());
		this.bundleContext.getServiceReferences(SI.class.getName(), "(attr=value)");
		this.mockContextControl.setReturnValue(new ServiceReference[0]);
		this.bundleContext.getAllServiceReferences(SI.class.getName(), "(attr=value)");
		this.mockContextControl.setReturnValue(new ServiceReference[0]);
		this.bundleContext.getServiceReferences(SI.class.getName(), "(attr=value)");
		this.mockContextControl.setReturnValue(new ServiceReference[0]);
		this.bundleContext.getAllServiceReferences(SI.class.getName(), "(attr=value)");
		this.mockContextControl.setReturnValue(new ServiceReference[0]);
		this.bundleContext.getServiceReferences(SI.class.getName(), "(attr=value)");
		this.mockContextControl.setReturnValue(new ServiceReference[0]);
		this.bundleContext.getAllServiceReferences(SI.class.getName(), "(attr=value)");
		this.mockContextControl.setReturnValue(new ServiceReference[0]);
		this.mockContextControl.replay();

		createInterceptor();
		try {
			this.interceptor.before(null, null, null);
			fail("should have thrown ServiceUnavailableException");
		}
		catch (ServiceUnavailableException ex) {
			assertTrue("Message should start with 'The target OSGi service",
				ex.getMessage().startsWith("The target OSGi service"));
			assertEquals(SI.class, ex.getServiceType());
			assertEquals("(attr=value)", ex.getFilter());
		}

		this.mockContextControl.verify();

	}

	public void testDummy() {

	}

	private ServiceReference getServiceReference() {
		MockControl sRefControl = MockControl.createNiceControl(ServiceReference.class);
		return (ServiceReference) sRefControl.getMock();
	}

	public interface SI {
		void process();
	}

}
