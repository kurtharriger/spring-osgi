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
package org.springframework.osgi.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.osgi.context.DelegatedExecutionOsgiBundleApplicationContext;
import org.springframework.osgi.context.OsgiBundleApplicationContextExecutor;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.ObjectUtils;

/**
 * OSGi-specific application context that delegates the execution of its
 * lifecycle methods to a different class. The main reason behind this is to
 * 'break' the startup of the application context in steps that can be executed
 * asynchronously.
 * 
 * <p/> The {@link #refresh()} and {@link #close()} methods delegate their
 * execution to an {@link OsgiBundleApplicationContextExecutor} class that
 * choses how to call the lifecycle submethods.
 * 
 * <p/> One can still call the 'traditional' lifecycle methods through
 * {@link #normalRefresh()} and {@link #normalClose()}.
 * 
 * @see DelegatedExecutionOsgiBundleApplicationContext
 * @author Costin Leau
 * 
 */
public abstract class AbstractDelegatedExecutionApplicationContext extends AbstractOsgiBundleApplicationContext
		implements DelegatedExecutionOsgiBundleApplicationContext {

	/**
	 * Synchronous refresh executor (provides normal behavior).
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class SynchronousRefreshExecutor implements OsgiBundleApplicationContextExecutor {

		private final DelegatedExecutionOsgiBundleApplicationContext context;

		private SynchronousRefreshExecutor(DelegatedExecutionOsgiBundleApplicationContext ctx) {
			context = ctx;
		}

		public void refresh() throws BeansException, IllegalStateException {
			context.normalRefresh();
		}

		public void close() {
			context.normalClose();
		}
	}

	/** Default executor */
	private OsgiBundleApplicationContextExecutor executor = new SynchronousRefreshExecutor(this);

	/** this context monitor */
	private final Object contextMonitor = new Object();

	/** monitor for available flag */
	private final Object availableMonitor = new Object();

	private boolean available = false;

	public boolean isAvailable() {
		synchronized (availableMonitor) {
			return available;
		}
	}

	/**
	 * Delegate execution of refresh method to a third party. This allows
	 * breaking the refresh process into several small pieces providing
	 * continuation-like behavior or completion of the refresh method on several
	 * threads, in a asynch manner.
	 * 
	 * By default, a {@link SynchronousRefreshExecutor} is used which executes
	 * the refresh method in one go (normal behavior).
	 */
	public void refresh() throws BeansException, IllegalStateException {
		executor.refresh();
	}

	public void normalRefresh() {
		Thread currentThread = Thread.currentThread();
		ClassLoader oldTCCL = currentThread.getContextClassLoader();

		try {
			currentThread.setContextClassLoader(getClassLoader());
			super.refresh();
		}
		finally {
			currentThread.setContextClassLoader(oldTCCL);
		}
	}

	public void normalClose() {
		Thread currentThread = Thread.currentThread();
		ClassLoader oldTCCL = currentThread.getContextClassLoader();

		try {
			currentThread.setContextClassLoader(getClassLoader());
			super.doClose();
		}
		finally {
			currentThread.setContextClassLoader(oldTCCL);
		}
	}

	// Adds behavior for isAvailable flag.
	protected void doClose() {
		synchronized (availableMonitor) {
			available = false;
		}
		executor.close();
	}

	public void preRefresh() {

		Thread thread = Thread.currentThread();
		ClassLoader oldTCCL = thread.getContextClassLoader();

		try {
			synchronized (contextMonitor) {
				thread.setContextClassLoader(getClassLoader());

				if (ObjectUtils.isEmpty(getConfigLocations())) {
					setConfigLocations(getDefaultConfigLocations());
				}
				if (!OsgiBundleUtils.isBundleActive(getBundle())) {
					throw new ApplicationContextException("Unable to refresh application context: bundle is "
							+ OsgiStringUtils.bundleStateAsString(getBundle()));
				}

				// Prepare this context for refreshing.
				prepareRefresh();

				// Tell the subclass to refresh the internal bean factory.
				ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

				// Prepare the bean factory for use in this context.
				prepareBeanFactory(beanFactory);

				try {
					// Allows post-processing of the bean factory in context
					// subclasses.
					postProcessBeanFactory(beanFactory);

					// Invoke factory processors registered as beans in the
					// context.
					invokeBeanFactoryPostProcessors(beanFactory);

				}
				catch (BeansException ex) {
					// Destroy already created singletons to avoid dangling
					// resources.
					beanFactory.destroySingletons();
					if (logger.isDebugEnabled()) {
						logger.debug("Post refresh error", ex);
					}
					throw ex;
				}
			}
		}
		finally {
			thread.setContextClassLoader(oldTCCL);
		}
	}

	public void postRefresh() {
		Thread thread = Thread.currentThread();
		ClassLoader oldTCCL = thread.getContextClassLoader();

		try {

			synchronized (contextMonitor) {
				thread.setContextClassLoader(getClassLoader());

				try {
					ConfigurableListableBeanFactory beanFactory = getBeanFactory();

					// Register bean processors that intercept bean creation.
					registerBeanPostProcessors(beanFactory);

					// Initialize message source for this context.
					initMessageSource();

					// Initialize event multicaster for this context.
					initApplicationEventMulticaster();

					// Initialize other special beans in specific context
					// subclasses.
					onRefresh();

					// Check for listener beans and register them.
					registerListeners();

					// Instantiate singletons this late to allow them to access
					// the
					// message source.

					beanFactory.preInstantiateSingletons();

					// Initialize other special beans in specific context
					// subclasses.
					onRefresh();

					// Check for listener beans and register them.
					registerListeners();

					// Instantiate all remaining (non-lazy-init) singletons.
					finishBeanFactoryInitialization(beanFactory);

					// Last step: publish corresponding event.
					finishRefresh();
				}
				catch (BeansException ex) {
					// Destroy already created singletons to avoid dangling
					// resources.
					getBeanFactory().destroySingletons();
					if (logger.isDebugEnabled()) {
						logger.debug("Post refresh error", ex);
					}
					throw ex;
				}
			}
		}
		finally {
			thread.setContextClassLoader(oldTCCL);
		}
	}

	protected void finishRefresh() {
		super.finishRefresh();

		synchronized (availableMonitor) {
			available = true;
		}
		// publish the context only after all the beans have been published
		publishContextAsOsgiServiceIfNecessary();
	}

	public Object getMonitor() {
		return contextMonitor;
	}

	public void setExecutor(OsgiBundleApplicationContextExecutor executor) {
		this.executor = executor;
	}

}
