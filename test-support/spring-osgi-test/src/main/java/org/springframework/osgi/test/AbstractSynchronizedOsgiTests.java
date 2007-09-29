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
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.util.concurrent.Counter;

/**
 * JUnit superclass which offers synchronization for bundle initialization. It
 * provides utility waiting methods for bundle initialization.
 * 
 * @author Costin Leau
 * @author Adrian Colyer
 * 
 */
public abstract class AbstractSynchronizedOsgiTests extends AbstractConfigurableOsgiTests {

	protected static final long DEFAULT_WAIT_TIME = 60L;

	private static final long SECOND = 1000;

	public AbstractSynchronizedOsgiTests() {
		super();
	}

	public AbstractSynchronizedOsgiTests(String name) {
		super(name);
	}

	/**
	 * Place the current (test) thread to wait for the a Spring application
	 * context to be published under the given symbolic name. This method allows
	 * waiting for full initialization of Spring OSGi bundles before starting
	 * the actual test execution.
	 * 
	 * @param context
	 * @param forBundleWithSymbolicName
	 * @param timeout
	 */
	public void waitOnContextCreation(String forBundleWithSymbolicName, long timeout) {
		waitOnContextCreation(bundleContext, forBundleWithSymbolicName, timeout);

	}

	/**
	 * Place the current (test) thread to wait for the a Spring application
	 * context to be published under the given symbolic name. This method allows
	 * waiting for full initialization of Spring OSGi bundles before starting
	 * the actual test execution.
	 * 
	 * @param context
	 * @param forBundleWithSymbolicName
	 * @param timeout
	 */
	public void waitOnContextCreation(BundleContext context, String forBundleWithSymbolicName, long timeout) {
		// translate from seconds to miliseconds
		long time = timeout * SECOND;

		// use the counter to make sure the threads block
		final Counter counter = new Counter("waitForContext on bnd=" + forBundleWithSymbolicName);

		counter.increment();

		String filter = "(org.springframework.context.service.name=" + forBundleWithSymbolicName + ")";

		ServiceListener listener = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				if (event.getType() == ServiceEvent.REGISTERED)
					counter.decrement();
			}
		};

		OsgiListenerUtils.addServiceListener(context, listener, filter);

		if (logger.isDebugEnabled())
			logger.debug("start waiting for Spring/OSGi bundle=" + forBundleWithSymbolicName);

		try {
			if (counter.waitForZero(time)) {
				waitingFailed(forBundleWithSymbolicName);
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("found applicationContext for bundle=" + forBundleWithSymbolicName);
			}
		}
		finally {
			// inform waiting thread
			context.removeServiceListener(listener);
		}
	}

	/**
	 * 'Sugar' method - identical to waitOnContextCreation({@link #getBundleContext()},
	 * forBundleWithSymbolicName, {@link #getDefaultWaitTime()}).
	 * 
	 * @param forBundleWithSymbolicName
	 */
	public void waitOnContextCreation(String forBundleWithSymbolicName) {
		waitOnContextCreation(forBundleWithSymbolicName, getDefaultWaitTime());
	}

	private void waitingFailed(String bundleName) {
		logger.warn("waiting for applicationContext for bundle=" + bundleName + " timed out");

		throw new RuntimeException("Gave up waiting for application context for '" + bundleName + "' to be created");
	}

	/**
	 * Return the default waiting time in seconds for
	 * {@link #waitOnContextCreation(String)}. Subclasses should override this
	 * method if the {@link #DEFAULT_WAIT_TIME} is not enough. For more
	 * customization, consider setting
	 * {@link #shouldWaitForSpringBundlesContextCreation()} to false and using
	 * {@link #waitOnContextCreation(BundleContext, String, long)}.
	 * 
	 * @return the default wait time (in seconds) for each spring bundle context
	 * to be published as an OSGi service
	 */
	protected long getDefaultWaitTime() {
		return DEFAULT_WAIT_TIME;
	}

	/**
	 * Should the test class wait for the context creation of Spring/OSGi
	 * bundles before executing the tests or not? Default is true.
	 * 
	 * @return true if the test will wait for spring bundle context creation or
	 * false otherwise
	 */
	protected boolean shouldWaitForSpringBundlesContextCreation() {
		return true;
	}

	/**
	 * Take care of waiting for Spring powered bundle application context
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
				String bundleName = OsgiStringUtils.nullSafeSymbolicName(bundle);
				if (OsgiBundleUtils.isBundleActive(bundle)) {
					if (ConfigUtils.isSpringOsgiPoweredBundle(bundle)
							&& ConfigUtils.getPublishContext(bundle.getHeaders())) {
						if (debug)
							logger.debug("Bundle [" + bundleName + "] triggers a context creation; waiting for it");
						// use platformBundleContext
						waitOnContextCreation(platformBundleContext, bundleName, getDefaultWaitTime());
					}
					else if (trace)
						logger.trace("Bundle [" + bundleName + "] does not trigger a context creation.");
				}
				else {
					if (trace)
						logger.trace("Bundle [" + bundleName + "] is not active (probably a fragment); ignoring");
				}
			}
		}
	}
}
