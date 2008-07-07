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

package org.springframework.osgi.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.osgi.context.DelegatedExecutionOsgiBundleApplicationContext;
import org.springframework.osgi.context.OsgiBundleApplicationContextExecutor;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticaster;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticasterAdapter;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.util.internal.ClassUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * OSGi-specific application context that delegates the execution of its life
 * cycle methods to a different class. The main reason behind this is to
 * <em>break</em> the startup of the application context in steps that can be
 * executed asynchronously.
 * 
 * <p/> The {@link #refresh()} and {@link #close()} methods delegate their
 * execution to an {@link OsgiBundleApplicationContextExecutor} class that
 * chooses how to call the lifecycle methods.
 * 
 * <p/> One can still call the 'traditional' lifecycle methods through
 * {@link #normalRefresh()} and {@link #normalClose()}.
 * 
 * @see DelegatedExecutionOsgiBundleApplicationContext
 * 
 * @author Costin Leau
 */
public abstract class AbstractDelegatedExecutionApplicationContext extends AbstractOsgiBundleApplicationContext
		implements DelegatedExecutionOsgiBundleApplicationContext {

	/**
	 * Executor that offers the traditional way of <code>refreshing</code>/<code>closing</code>
	 * of an ApplicationContext (no conditions have to be met and the refresh
	 * happens in only one step).
	 * 
	 * @author Costin Leau
	 */
	private static class NoDependenciesWaitRefreshExecutor implements OsgiBundleApplicationContextExecutor {

		private final DelegatedExecutionOsgiBundleApplicationContext context;


		private NoDependenciesWaitRefreshExecutor(DelegatedExecutionOsgiBundleApplicationContext ctx) {
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
	private OsgiBundleApplicationContextExecutor executor = new NoDependenciesWaitRefreshExecutor(this);

	/** this context monitor */
	private final Object contextMonitor = new Object();

	/** monitor for available flag */
	private final Object availableMonitor = new Object();

	private boolean available = false;

	/** delegated multicaster */
	private OsgiBundleApplicationContextEventMulticaster delegatedMulticaster;

	private ContextClassLoaderProvider cclProvider;


	/**
	 * 
	 * Create a new AbstractDelegatedExecutionApplicationContext with no parent.
	 * 
	 */
	public AbstractDelegatedExecutionApplicationContext() {
		super();
	}

	/**
	 * Create a new AbstractDelegatedExecutionApplicationContext with the given
	 * parent context.
	 * 
	 * @param parent the parent context
	 */
	public AbstractDelegatedExecutionApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	boolean isAvailable() {
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
	 * By default, the refresh method in executed in <em>one go</em> (normal
	 * behavior).
	 * 
	 * {@inheritDoc}
	 */
	public void refresh() throws BeansException, IllegalStateException {
		executor.refresh();
	}

	public void normalRefresh() {
		Assert.notNull(getBundleContext(), "bundle context should be set before refreshing the application context");

		Thread currentThread = Thread.currentThread();
		ClassLoader oldTCCL = currentThread.getContextClassLoader();

		try {
			currentThread.setContextClassLoader(contextClassLoaderProvider().getContextClassLoader());
			try {
				super.refresh();
				sendRefreshedEvent();
			}
			catch (RuntimeException ex) {
				logger.error("Refresh error", ex);
				sendFailedEvent(ex);
				// propagate exception to the caller
				throw ex;
			}
		}
		finally {
			currentThread.setContextClassLoader(oldTCCL);
		}
	}

	public void normalClose() {
		Thread currentThread = Thread.currentThread();
		ClassLoader oldTCCL = currentThread.getContextClassLoader();

		try {
			currentThread.setContextClassLoader(contextClassLoaderProvider().getContextClassLoader());
			super.doClose();
		}
		finally {
			currentThread.setContextClassLoader(oldTCCL);
		}
	}

	// Adds behaviour for isAvailable flag.
	protected void doClose() {
		synchronized (availableMonitor) {
			available = false;
		}
		executor.close();
	}

	public void startRefresh() {

		// check concurrent collection (which are mandatory)
		if (!ClassUtils.concurrentLibAvailable())
			throw new IllegalStateException(
				"JVM 5+ or backport-concurrent library (for JVM 1.4) required; see the FAQ for more details");

		Thread thread = Thread.currentThread();
		ClassLoader oldTCCL = thread.getContextClassLoader();

		try {
			synchronized (contextMonitor) {
				thread.setContextClassLoader(contextClassLoaderProvider().getContextClassLoader());

				if (ObjectUtils.isEmpty(getConfigLocations())) {
					setConfigLocations(getDefaultConfigLocations());
				}
				if (!OsgiBundleUtils.isBundleActive(getBundle())) {
					throw new ApplicationContextException("Unable to refresh application context: bundle is "
							+ OsgiStringUtils.bundleStateAsString(getBundle()));
				}

				ConfigurableListableBeanFactory beanFactory = null;
				// Prepare this context for refreshing.
				prepareRefresh();

				// Tell the subclass to refresh the internal bean factory.
				beanFactory = obtainFreshBeanFactory();

				// Prepare the bean factory for use in this context.
				prepareBeanFactory(beanFactory);

				try {
					// Allows post-processing of the bean factory in context
					// subclasses.
					postProcessBeanFactory(beanFactory);

					// Invoke factory processors registered as beans in the
					// context.
					invokeBeanFactoryPostProcessors(beanFactory);

					// Register bean processors that intercept bean creation.
					registerBeanPostProcessors(beanFactory);

				}
				catch (BeansException ex) {
					// Destroy already created singletons to avoid dangling
					// resources.
					beanFactory.destroySingletons();
					cancelRefresh(ex);
					// propagate exception to the caller
					throw ex;
				}
			}
		}
		catch (RuntimeException ex) {
			logger.error("Pre refresh error", ex);
			// send failure event
			sendFailedEvent(ex);
			throw ex;
		}
        catch (Error err) {
            logger.error("Pre refresh error", err);
            // send failure event
            sendFailedEvent(err);
            throw err;
        }
		finally {
			thread.setContextClassLoader(oldTCCL);
		}
	}

	public void completeRefresh() {
		Thread thread = Thread.currentThread();
		ClassLoader oldTCCL = thread.getContextClassLoader();

		try {

			synchronized (contextMonitor) {
				thread.setContextClassLoader(contextClassLoaderProvider().getContextClassLoader());

				try {
					ConfigurableListableBeanFactory beanFactory = getBeanFactory();

					// Initialize message source for this context.
					initMessageSource();

					// Initialize event multicaster for this context.
					initApplicationEventMulticaster();

					// Initialize other special beans in specific context
					// subclasses.
					onRefresh();

					// Check for listener beans and register them.
					registerListeners();

					// Instantiate all remaining (non-lazy-init) singletons.
					finishBeanFactoryInitialization(beanFactory);

					// Last step: publish corresponding event.
					finishRefresh();

					// everything went okay, post notification
					sendRefreshedEvent();
				}
				catch (BeansException ex) {
					// Destroy already created singletons to avoid dangling
					// resources.
					getBeanFactory().destroySingletons();
					cancelRefresh(ex);
					// propagate exception to the caller
					throw ex;
				}
			}
		}
		catch (RuntimeException ex) {
			logger.error("Post refresh error", ex);
			// post notification
			sendFailedEvent(ex);
			throw ex;
		}
        catch (Error err) {
            logger.error("Post refresh error", err);
            // post notification
            sendFailedEvent(err);
            throw err;
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

	public void setDelegatedEventMulticaster(OsgiBundleApplicationContextEventMulticaster multicaster) {
		this.delegatedMulticaster = multicaster;
	}

	/**
	 * Sets the OSGi multicaster by using a Spring
	 * {@link ApplicationEventMulticaster}. This method is added as a
	 * covenience.
	 * 
	 * @param multicaster Spring multi-caster used for propagating OSGi specific
	 * events
	 * 
	 * @see OsgiBundleApplicationContextEventMulticasterAdapter
	 */
	public void setDelegatedEventMulticaster(ApplicationEventMulticaster multicaster) {
		this.delegatedMulticaster = new OsgiBundleApplicationContextEventMulticasterAdapter(multicaster);
	}

	public OsgiBundleApplicationContextEventMulticaster getDelegatedEventMulticaster() {
		return this.delegatedMulticaster;
	}

	private void sendFailedEvent(Throwable cause) {
		if (delegatedMulticaster != null)
			delegatedMulticaster.multicastEvent(new OsgiBundleContextFailedEvent(this, this.getBundle(), cause));
	}

	private void sendRefreshedEvent() {
		if (delegatedMulticaster != null)
			delegatedMulticaster.multicastEvent(new OsgiBundleContextRefreshedEvent(this, this.getBundle()));
	}

	/**
	 * Returns the context class loader to be used as the Thread Context Class
	 * Loader for {@link #refresh()} and {@link #destroy()} calls.
	 * 
	 * The default implementation returns the bean class loader if it is set or
	 * or the current context class loader otherwise.
	 * 
	 * @return the thread context class loader to be used during the execution
	 * of critical section blocks
	 * @deprecated will be removed after RC1 is released
	 */
	protected ClassLoader getContextClassLoader() {
		return contextClassLoaderProvider().getContextClassLoader();
	}

	/** private method used for doing lazy-init-if-not-set for cclProvider */
	private ContextClassLoaderProvider contextClassLoaderProvider() {
		if (cclProvider == null) {
			DefaultContextClassLoaderProvider defaultProvider = new DefaultContextClassLoaderProvider();
			defaultProvider.setBeanClassLoader(getClassLoader());
			cclProvider = defaultProvider;
		}
		return cclProvider;
	}

	/**
	 * Sets the {@link ContextClassLoaderProvider} used by this OSGi application
	 * context instance. By default, {@link DefaultContextClassLoaderProvider}
	 * is used.
	 * 
	 * @param contextClassLoaderProvider context class loader provider to use
	 * @see ContextClassLoaderProvider
	 * @see DefaultContextClassLoaderProvider
	 */
	public void setContextClassLoaderProvider(ContextClassLoaderProvider contextClassLoaderProvider) {
		this.cclProvider = contextClassLoaderProvider;
	}
}
