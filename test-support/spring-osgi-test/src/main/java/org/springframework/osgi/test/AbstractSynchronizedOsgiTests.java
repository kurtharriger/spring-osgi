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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.springframework.osgi.util.ConfigUtils;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiListenerUtils;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.util.ObjectUtils;

import edu.emory.mathcs.backport.java.util.concurrent.BrokenBarrierException;
import edu.emory.mathcs.backport.java.util.concurrent.CyclicBarrier;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;

/**
 * JUnit superclass which offers synchronization for bundle initialization. It
 * provides utility waiting methods for bundle initialization.
 * 
 * @author Costin Leau
 * @author Adrian Colyer
 * 
 */
public abstract class AbstractSynchronizedOsgiTests extends AbstractConfigurableOsgiTests {

	private static final long DEFAULT_WAIT_TIME = 5L;

	private static final long DEFAULT_SLEEP_INTERVAL = 500;

	private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

	/**
	 * Service listener searching for ApplicationContext published as services
	 * with the given symbolic name.
	 * 
	 * @author Adrian Colyer
	 * 
	 */
	private class ApplicationContextWaiter implements Runnable, ServiceListener {

		private final String symbolicName;

		private final CyclicBarrier barrier;

		private boolean foundService = false;

		private Boolean[] shouldStop;

		private BundleContext context;

		public ApplicationContextWaiter(BundleContext context, CyclicBarrier barrier, String bundleSymbolicName,
				Boolean[] shouldStop) {
			this.symbolicName = bundleSymbolicName;
			this.barrier = barrier;
			this.shouldStop = shouldStop;
			this.context = context;
		}

		public void run() {

			String filter = "(org.springframework.context.service.name=" + symbolicName + ")";

			OsgiListenerUtils.addServiceListener(context, this, filter);

			// now look and see if the service was already registered before
			// we even got here...
			if (!ObjectUtils.isEmpty(OsgiServiceReferenceUtils.getServiceReferences(context,
				"org.springframework.context.ApplicationContext", filter))) {
				endWait();
			}

			try {
				// do the waiting on this thread
				for (; !(shouldStop[0].booleanValue() || foundService);) {
					Thread.sleep(DEFAULT_SLEEP_INTERVAL);
				}

				if (!shouldStop[0].booleanValue())
					barrier.await(DEFAULT_WAIT_TIME, DEFAULT_TIME_UNIT);
			}
			catch (Exception ex) {
				// got exception
				System.err.println("got exception " + ex);
			}
		}

		public void serviceChanged(ServiceEvent event) {
			if (event.getType() == ServiceEvent.REGISTERED) {
				// our wait is over...
				endWait();
			}
		}

		private void endWait() {
			foundService = true;
			if (logger.isDebugEnabled())
				logger.debug("found appCtx for [" + symbolicName + "]");
		}
	}

	public AbstractSynchronizedOsgiTests() {
		super();
	}

	public AbstractSynchronizedOsgiTests(String name) {
		super(name);
	}

	public void waitOnContextCreation(BundleContext context, String forBundleWithSymbolicName, long timeout,
			TimeUnit unit) {
		// use a barrier to ensure we don't proceed until context is published
		CyclicBarrier barrier = new CyclicBarrier(2);
		// pass the boolean as an array to be able to modify it per-thread basis
		Boolean[] shouldStop = new Boolean[] { Boolean.FALSE };

		ApplicationContextWaiter waiter = new ApplicationContextWaiter(context, barrier, forBundleWithSymbolicName,
				shouldStop);
		Thread waitThread = new Thread(waiter);
		waitThread.setName("[" + forBundleWithSymbolicName + "] appCtx waiter");
		waitThread.setDaemon(true);

		if (logger.isDebugEnabled())
			logger.debug("start waiting for Spring/OSGi bundle=" + forBundleWithSymbolicName);

		waitThread.start();

		try {
			// test thread
			barrier.await(timeout, unit);
			if (logger.isDebugEnabled())
				logger.debug("found applicationContext for bundle=" + forBundleWithSymbolicName);
		}
		catch (BrokenBarrierException ex) {
			barrierFailed(ex, forBundleWithSymbolicName);
		}

		catch (InterruptedException ex) {
			barrierFailed(ex, forBundleWithSymbolicName);
		}

		catch (TimeoutException ex) {
			barrierFailed(null, forBundleWithSymbolicName);
		}

		finally {
			// inform waiting thread
			shouldStop[0] = Boolean.TRUE;
			context.removeServiceListener(waiter);
			barrier.reset();
		}
	}

	private void barrierFailed(Exception cause, String bundleName) {
		/*
		 * Map<Thread, StackTraceElement[]> stacks =
		 * Thread.getAllStackTraces(); int t = 0; for (StackTraceElement[] i :
		 * stacks.values()) { System.out.println("Thread [" + t + "]"); for (int
		 * j = 0 ; j<i.length; j++) { System.out.println(" " + i[j]); } t++; }
		 */

		if (logger.isDebugEnabled())
			logger.debug("waiting for applicationContext for bundle=" + bundleName + " timed out");

		throw new RuntimeException("Gave up waiting for application context for '" + bundleName + "' to be created",
				cause);
	}

	public void waitOnContextCreation(String forBundleWithSymbolicName) {
		waitOnContextCreation(getBundleContext(), forBundleWithSymbolicName, DEFAULT_WAIT_TIME, DEFAULT_TIME_UNIT);
	}

	/**
	 * Should the test class wait for the context creation of Spring/OSGi
	 * bundles before executing the tests or not? Default is true.
	 * 
	 * @return
	 */
	protected boolean shouldWaitForSpringBundlesContextCreation() {
		return true;
	}

	/**
	 * Takes care of waiting for Spring powered bundle application context
	 * creation.
	 */
	protected void postProcessBundleContext(BundleContext platformBundleContext) throws Exception {
		if (shouldWaitForSpringBundlesContextCreation()) {
			boolean debug = logger.isDebugEnabled();
			boolean trace = logger.isTraceEnabled();
			if (debug)
				logger.debug("looking for Spring/OSGi powered bundles to wait for...");

			// determine Spring/OSGi bundles
			Bundle[] bundles = platformBundleContext.getBundles();
			for (int i = 0; i < bundles.length; i++) {
				Bundle bundle = bundles[i];
				String bundleName = OsgiBundleUtils.getNullSafeSymbolicName(bundle);
				if (OsgiBundleUtils.isBundleActive(bundle)) {
					if (ConfigUtils.isSpringOsgiPoweredBundle(bundle)) {
						if (debug)
							logger.debug("Bundle [" + bundleName + "] is Spring/OSGi powered; waiting for it");
						waitOnContextCreation(platformBundleContext, bundleName, DEFAULT_WAIT_TIME, DEFAULT_TIME_UNIT);
					}
					else if (trace)
						logger.trace("Bundle [" + bundleName + "] is not Spring/OSGi powered");
				}
				else {
					if (trace)
						logger.trace("Bundle [" + bundleName + "] is not active; ignoring");

				}
			}
		}

	}
}
