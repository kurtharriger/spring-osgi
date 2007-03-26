/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

import edu.emory.mathcs.backport.java.util.concurrent.BrokenBarrierException;
import edu.emory.mathcs.backport.java.util.concurrent.CyclicBarrier;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * JUnit superclass which offers synchronization for bundle initialization. It
 * provides utility waiting methods for bundle initialization.
 * 
 * @author Costin Leau
 * @author Adrian Colyer
 * 
 */
public abstract class AbstractSynchronizedOsgiTests extends AbstractConfigurableOsgiTests {

	private static class ApplicationContextWaiter implements Runnable, ServiceListener {

		private final String symbolicName;

		private final CyclicBarrier barrier;

		private final BundleContext context;

		public ApplicationContextWaiter(CyclicBarrier barrier, BundleContext context, String bundleSymbolicName) {
			this.symbolicName = bundleSymbolicName;
			this.barrier = barrier;
			this.context = context;
		}

		public void run() {
			String filter = "(org.springframework.context.service.name=" + symbolicName + ")";
			try {
				context.addServiceListener(this, filter);
				// now look and see if the service was already registered before
				// we even got here...
				if (this.context.getServiceReferences("org.springframework.context.ApplicationContext", filter) != null) {
					returnControl();
				}
			}
			catch (InvalidSyntaxException badSyntaxEx) {
				throw new IllegalStateException("OSGi runtime rejected filter '" + filter + "'");
			}
		}

		public void serviceChanged(ServiceEvent event) {
			if (event.getType() == ServiceEvent.REGISTERED) {
				// our wait is over...
				returnControl();
			}
		}

		private void returnControl() {
			this.context.removeServiceListener(this);
			try {
				this.barrier.await();
			}
			catch (BrokenBarrierException ex) {
				// return;
			}
			catch (InterruptedException intEx) {
				// return;
			}
		}
	}
	
	
	public AbstractSynchronizedOsgiTests() {
		super();
	}

	public AbstractSynchronizedOsgiTests(String name) {
		super(name);
	}

	public void waitOnContextCreation(String forBundleWithSymbolicName, long timeout, TimeUnit unit) {
		// use a barrier to ensure we don't proceed until context is published
		final CyclicBarrier barrier = new CyclicBarrier(2);

		Thread waitThread = new Thread(new ApplicationContextWaiter(barrier, getBundleContext(), forBundleWithSymbolicName));
		waitThread.start();

		try {
			barrier.await(timeout, unit);
		}
		catch (Throwable e) {
            /*
            Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
            int t = 0;
            for (StackTraceElement[] i : stacks.values()) {
                System.out.println("Thread [" + t + "]");
                for (int j = 0 ; j<i.length; j++) {
                    System.out.println("  " + i[j]);
                }
                t++;
            }
            */
            throw new RuntimeException("Gave up waiting for application context for '" + forBundleWithSymbolicName
					+ "' to be created");
		}
	}

	public void waitOnContextCreation(String forBundleWithSymbolicName) {
		waitOnContextCreation(forBundleWithSymbolicName, 5L, TimeUnit.SECONDS);
	}

	//FIXME: automatically determine the Spring bundles installed and wait for them

}
