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
 */
package org.springframework.osgi.extender;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.springframework.core.CollectionFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.support.AbstractDelegatedExecutionApplicationContext;
import org.springframework.osgi.context.support.ApplicationContextConfiguration;
import org.springframework.osgi.context.support.NamespacePlugins;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.extender.dependencies.shutdown.ComparatorServiceDependencySorter;
import org.springframework.osgi.extender.dependencies.shutdown.ServiceDependencySorter;
import org.springframework.osgi.extender.dependencies.startup.DependencyWaiterApplicationContextExecutor;
import org.springframework.osgi.internal.extender.util.ConfigUtils;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiPlatformDetector;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.util.concurrent.Counter;
import org.springframework.osgi.util.concurrent.RunnableTimedExecution;
import org.springframework.scheduling.timer.TimerTaskExecutor;
import org.springframework.util.Assert;

/**
 * Osgi Extender that bootstraps 'Spring powered bundles'.
 * 
 * <p/>
 * 
 * <p/> The class listens to bundle events and manages the creation and
 * destruction of application contexts for bundles that have one or both of:
 * <ul>
 * <li> A manifest header entry Spring-Context
 * <li> XML files in META-INF/spring
 * </ul>
 * <p/> The extender also discovers any Spring namespace handlers in resolved
 * bundles and publishes a namespace resolving service for each.
 * </p>
 * <p/>
 * 
 * <p/> If a fragment is attached to the extender bundle that defines a resource
 * META-INF/spring/extender.xml then this file will be used to create an
 * application context for configuring the extender bundle itself. By defining a
 * bean named "taskExecutor" in that context you can configure how the extender
 * bundle schedules asynchronous activity. The extender context is created
 * during a synchronous OSGi lifecycle callback and should contain only simple
 * bean definitions that will not delay context initialisation.
 * </p>
 * 
 * @author Bill Gallagher
 * @author Andy Piper
 * @author Hal Hildebrand
 * @author Adrian Colyer
 * @author Costin Leau
 */
public class ContextLoaderListener implements BundleActivator {

	private class BundleListener implements SynchronousBundleListener {
		/**
		 * A bundle has been started, stopped, resolved, or unresolved. This
		 * method is a synchronous callback, do not do any long-running work in
		 * this thread.
		 * 
		 * @see org.osgi.framework.SynchronousBundleListener#bundleChanged
		 */
		public void bundleChanged(BundleEvent event) {

			boolean debug = log.isDebugEnabled();
			boolean trace = log.isTraceEnabled();

			// check if we are being shutdown
			synchronized (monitor) {
				if (isClosed) {
					if (trace)
						log.trace("listener is closed; events are being ignored");
					return;
				}
			}

			try {
				Bundle bundle = event.getBundle();

				if (bundle.getBundleId() == bundleId) {
					return;
				}

				if (trace) {
					log.debug("Processing bundle event [" + OsgiStringUtils.nullSafeToString(event) + "] for bundle ["
							+ OsgiStringUtils.nullSafeSymbolicName(bundle) + "]");
				}

				switch (event.getType()) {
				case BundleEvent.STARTED: {
					maybeCreateApplicationContextFor(bundle);
					break;
				}
				case BundleEvent.STOPPING: {
					if (OsgiBundleUtils.isSystemBundle(bundle)) {
						if (debug) {
							log.debug("System bundle stopping");
						}
						// System bundle is shutting down; Special handling for
						// framework shutdown
						shutdown();
					}
					else {
						maybeCloseApplicationContextFor(bundle);
					}
					break;
				}
				case BundleEvent.RESOLVED: {
					maybeAddNamespaceHandlerFor(bundle);
					break;
				}
				case BundleEvent.UNRESOLVED: {
					maybeRemoveNameSpaceHandlerFor(bundle);
					break;
				}
				default:
					break;
				}
			}
			catch (Throwable e) {
				// OSGi frameworks simply swallow these exceptions
				log.fatal("Exception handing bundle changed event", e);
			}
		}
	}

	// The standard for META-INF header keys excludes ".", so these constants
	// must use "-"
	protected static final String SPRING_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";

	protected static final String EXTENDER_CONFIG_FILE_LOCATION = "META-INF/spring/extender.xml";

	protected static final String[] OSGI_BUNDLE_RESOLVER_INTERFACE_NAME = { "org.springframework.osgi.context.support.OsgiBundleNamespaceHandlerAndEntityResolver" };

	protected static final String TASK_EXECUTOR_BEAN_NAME = "taskExecutor";

	private static final Log log = LogFactory.getLog(ContextLoaderListener.class);

	// "Spring Application Context Creation Timer"
	protected Timer timer = new Timer(true);

	/**
	 * The id of the extender bundle itself
	 */
	protected long bundleId;

	/**
	 * Context created to configure the extender bundle itself (currently only
	 * used for overriding task executor implementation).
	 */
	protected OsgiBundleXmlApplicationContext extenderContext;

	/**
	 * The contexts we are currently managing. Keys are bundle ids, values are
	 * ServiceDependentOsgiApplicationContexts for the application context
	 */
	protected final Map managedContexts;

	/**
	 * ServiceRegistration object returned by OSGi when registering the
	 * NamespacePlugins instance as a service
	 */
	protected ServiceRegistration resolverServiceRegistration = null;

	/**
	 * List of listeners subscribed to spring bundle events
	 */
	protected final Set springBundleListeners = new LinkedHashSet();

	/**
	 * Are we running under knoplerfish? Required for bug workaround with
	 * calling getResource under KF
	 */
	protected boolean isKnopflerfish = false;

	/**
	 * The set of all namespace plugins known to the extender
	 */
	protected NamespacePlugins namespacePlugins;

	/** Task executor used for bootstraping the Spring contexts */
	protected TaskExecutor taskExecutor;

	/**
	 * Synchronous task executor using a background thread (timer).
	 * 
	 * Acts as a queue for starting bundles in a synchronized, serialized
	 * manner.
	 */
	private TimerTaskExecutor syncTaskExecutor;

	/**
	 * Task executor which uses the same thread for running tasks. Used when
	 * doing a synchronous wait-for-dependencies.
	 * 
	 */
	private TaskExecutor sameThreadTaskExecutor = new SyncTaskExecutor();

	/** ThreadGroup when the taskExecutor is created internally */
	private ThreadGroup threadGroup;

	/** is this class internally managing the task executor */
	protected boolean isTaskExecutorManagedInternally = false;

	/** listener counter - used to properly synchronize shutdown */
	protected Counter contextsStarted = new Counter("contextsStarted");

	/**
	 * ServiceDependency for dependencyDetector management
	 */
	protected ServiceRegistration listenerServiceRegistration;

	/**
	 * The bundle's context
	 */
	protected BundleContext context;

	/** Bundle listener */
	private SynchronousBundleListener bundleListener;

	/** Service-based dependency sorter for shutdown */
	private ServiceDependencySorter shutdownDependencySorter = new ComparatorServiceDependencySorter();

	/**
	 * Monitor used for dealing with the bundle activator and synchronous bundle
	 * threads
	 */
	private transient final Object monitor = new Object();

	private boolean isClosed = false;

	/** wait 3 seconds for each context to close */
	private static long SHUTDOWN_WAIT_TIME = 3 * 1000;

	/** this extender versin */
	private Version extenderVersion;

	/*
	 * Required by the BundleActivator contract
	 */
	public ContextLoaderListener() {
		this.managedContexts = CollectionFactory.createConcurrentMapIfPossible(30);
		syncTaskExecutor = new TimerTaskExecutor();
		syncTaskExecutor.afterPropertiesSet();
	}

	/**
	 * <p/> Called by OSGi when this bundle is started. Finds all previously
	 * resolved bundles and adds namespace handlers for them if necessary.
	 * </p>
	 * <p/> Creates application contexts for bundles started before the extender
	 * was started.
	 * </p>
	 * <p/> Registers a namespace/entity resolving service for use by web app
	 * contexts.
	 * </p>
	 * 
	 * @see org.osgi.framework.BundleActivator#start
	 */
	public void start(BundleContext context) throws Exception {
		this.extenderVersion = OsgiBundleUtils.getBundleVersion(context.getBundle());

		log.info("Starting org.springframework.osgi.extender bundle v.[" + extenderVersion + "]");

		this.isKnopflerfish = OsgiPlatformDetector.isKnopflerfish(context);

		this.context = context;
		this.bundleId = context.getBundle().getBundleId();
		this.namespacePlugins = new NamespacePlugins();
		
		// Collect all previously resolved bundles which have namespace
		// plugins
		Bundle[] previousBundles = context.getBundles();
		for (int i = 0; i < previousBundles.length; i++) {
			Bundle bundle = previousBundles[i];
			// do matching on extender version
			if (OsgiBundleUtils.isBundleResolved(bundle)) {
				maybeAddNamespaceHandlerFor(bundle);
			}
		}

		// TODO: possible race condition - bundles resolved between these two
		// calls
		// will not be considered for namespace handling registration.

		this.resolverServiceRegistration = registerResolverService(context);
		// do this once namespace handlers have been detected
		this.taskExecutor = createTaskExecutor(context);

		// make sure to register this before any listening starts
		// registerShutdownHook();

		// first register the service listener to make sure that we
		// catch any listener in starting phase
		registerListenerService(context);

		bundleListener = new BundleListener();

		// listen to any changes in bundles
		context.addBundleListener(bundleListener);

		// Instantiate all previously resolved bundles which are Spring
		// powered
		for (int i = 0; i < previousBundles.length; i++) {
			if (OsgiBundleUtils.isBundleActive(previousBundles[i])) {
				maybeCreateApplicationContextFor(previousBundles[i]);
			}
		}
	}

	/**
	 * Called by OSGi when this bundled is stopped. Unregister the
	 * namespace/entity resolving service and clear all state. No further
	 * management of application contexts created by this extender prior to
	 * stopping the bundle occurs after this point (even if the extender bundle
	 * is subsequently restarted).
	 * 
	 * @see org.osgi.framework.BundleActivator#stop
	 */
	public void stop(BundleContext context) throws Exception {
		shutdown();
	}

	/**
	 * Shutdown the extender and all bundled managed by it. Shutdown of contexts
	 * is in the topological order of the dependency graph formed by the service
	 * references.
	 */
	protected synchronized void shutdown() {
		synchronized (monitor) {
			// if already closed, bail out
			if (isClosed)
				return;
			else
				isClosed = true;
		}
		log.info("Stopping org.springframework.osgi.extender bundle");

		// first stop the watchdog
		stopTimer();
		// then the sync appCtx queue
		syncTaskExecutor.destroy();


		if (bundleListener != null) {
			context.removeBundleListener(bundleListener);
			bundleListener = null;
		}

		unregisterListenerService();
		unregisterResolverService();

		Bundle[] bundles = new Bundle[managedContexts.size()];

		int i = 0;
		for (Iterator it = managedContexts.values().iterator(); it.hasNext();) {
			ConfigurableOsgiBundleApplicationContext context = (ConfigurableOsgiBundleApplicationContext) it.next();
			bundles[i++] = context.getBundle();
		}

		bundles = shutdownDependencySorter.computeServiceDependencyGraph(bundles);

		boolean debug = log.isDebugEnabled();

		StringBuffer buffer = new StringBuffer();
		if (debug) {
			buffer.append("Shutdown order is: {");
			for (i = 0; i < bundles.length; i++) {
				buffer.append("\nBundle [" + bundles[i].getSymbolicName() + "]");
			}
			buffer.append("\n}");
			log.debug(buffer);
		}

		final List taskList = new ArrayList(managedContexts.size());
		final List closedContexts = Collections.synchronizedList(new ArrayList());
		final Object[] contextClosingDown = new Object[1];

		for (i = 0; i < bundles.length; i++) {
			Long id = new Long(bundles[i].getBundleId());
			final ConfigurableOsgiBundleApplicationContext context = (ConfigurableOsgiBundleApplicationContext) managedContexts.get(id);
			if (context != null) {
				closedContexts.add(context);
				// add a new runnable
				taskList.add(new Runnable() {
					public void run() {
						contextClosingDown[0] = context;
						// eliminate context
						closedContexts.remove(context);
						if (log.isDebugEnabled())
							log.debug("closing appCtx " + context.getDisplayName());
						context.close();
					}
				});
			}
		}

		// tasks
		final Runnable[] tasks = (Runnable[]) taskList.toArray(new Runnable[taskList.size()]);

		// start the ripper >:)
		for (int j = 0; j < tasks.length; j++) {
			if (RunnableTimedExecution.execute(tasks[j], SHUTDOWN_WAIT_TIME)) {
				if (debug) {
					log.debug(contextClosingDown[0] + " context did not closed succesfully; forcing shutdown");
				}
			}
		}

		this.managedContexts.clear();

		this.namespacePlugins = null;
		this.taskExecutor = null;
		if (this.extenderContext != null) {
			this.extenderContext.close();
			this.extenderContext = null;
		}

		// before bailing out; wait for the threads that might be left by
		// the
		// task executor
		stopTaskExecutor();

	}

	/**
	 * Cancel any tasks scheduled for the timer.
	 */
	private void stopTimer() {
		if (timer != null) {
			if (log.isDebugEnabled())
				log.debug("Canceling timer tasks");
			timer.cancel();
		}
		timer = null;
	}

	/**
	 * Shutdown the task executor in case is managed internally by the listener.
	 * 
	 */
	private void stopTaskExecutor() {
		boolean debug = log.isDebugEnabled();

		if (taskExecutor != null) {
			// only apply these when working with internally created task
			// executors
			if (isTaskExecutorManagedInternally) {

				if (debug)
					log.debug("Waiting for " + contextsStarted + " service dependency listener(s) to stop...");

				contextsStarted.waitForZero(SHUTDOWN_WAIT_TIME);

				if (!contextsStarted.isZero()) {
					if (debug)
						log.debug(contextsStarted.getValue()
								+ " service dependency listener(s) did not responded in time; forcing them to shutdown");
					if (threadGroup != null) {
						threadGroup.stop();
						threadGroup = null;
					}
				}

				else
					log.debug("all listeners closed");
			}
		}
	}

	/**
	 * Register a Spring applicationEvent multicaster for the managed bundles.
	 * 
	 * @param context
	 */
	// TODO: why not move the multicaster into its own class
	protected void registerListenerService(BundleContext context) {
		if (log.isDebugEnabled()) {
			log.debug("Registering Spring ContextListenerContext service");
		}

		// listenerServiceRegistration = context.registerService(
		// new String[] { ApplicationEventMulticaster.class.getName() }, this,
		// null);
	}

	/**
	 * Unregister the Spring application event multicaster for the managed
	 * Spring/OSGi applications.
	 */
	protected void unregisterListenerService() {
		if (log.isDebugEnabled()) {
			log.debug("Unregistering Spring ContextListenerContext service");
		}

		OsgiServiceUtils.unregisterService(listenerServiceRegistration);
		this.listenerServiceRegistration = null;
	}

	/**
	 * Register the NamespacePlugins instance as an Osgi Resolver service
	 */
	protected ServiceRegistration registerResolverService(BundleContext context) {
		if (log.isDebugEnabled()) {
			log.debug("Registering Spring NamespaceHandler and EntityResolver service");
		}

		return context.registerService(OSGI_BUNDLE_RESOLVER_INTERFACE_NAME, this.namespacePlugins, null);
	}

	/**
	 * Unregister the NamespaceHandler and EntityResolver service
	 */
	protected void unregisterResolverService() {
		if (log.isDebugEnabled()) {
			log.debug("Unregistering Spring NamespaceHandler and EntityResolver service");
		}

		OsgiServiceUtils.unregisterService(resolverServiceRegistration);
		this.resolverServiceRegistration = null;
	}

	/**
	 * Context creation is a potentially long-running activity (certainly more
	 * than we want to do on the synchronous event callback).
	 * 
	 * <p/>Based on our configuration, the context can be started on the same
	 * thread or on a different one.
	 * 
	 * Kick off a background activity to create an application context for the
	 * given bundle if needed.
	 * 
	 * @param bundle
	 */
	protected void maybeCreateApplicationContextFor(Bundle bundle) {

		if (!ConfigUtils.matchExtenderVersionRange(bundle, extenderVersion)) {
			if (log.isDebugEnabled())
				log.debug("bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle)
						+ "] expects an extender w/ version["
						+ OsgiBundleUtils.getHeaderAsVersion(bundle, ConfigUtils.EXTENDER_VERSION)
						+ "] which does not match current extender w/ version[" + extenderVersion
						+ "]; skipping bundle from context creation");
			return;
		}

		ApplicationContextConfiguration config = new ApplicationContextConfiguration(bundle);
		if (log.isDebugEnabled())
			log.debug("created config " + config);

		if (!config.isSpringPoweredBundle()) {
			return;
		}

		Long bundleId = new Long(bundle.getBundleId());

		final AbstractDelegatedExecutionApplicationContext context = createApplicationContext(
			OsgiBundleUtils.getBundleContext(bundle), config.getConfigurationLocations());

		if (managedContexts.containsKey(bundleId)) {
			if (log.isDebugEnabled()) {
				log.debug("Bundle is already under control: " + bundle.getSymbolicName());
			}
			return;
		}
		managedContexts.put(bundleId, context);

		context.setPublishContextAsService(config.isPublishContextAsService());
		context.setNamespaceResolver(namespacePlugins);

		// wait/no wait for dependencies behavior

		if (config.isWaitForDependencies()) {
			DependencyWaiterApplicationContextExecutor appCtxExecutor = new DependencyWaiterApplicationContextExecutor(
					context, !config.isCreateAsynchronously());

			appCtxExecutor.setTimeout(config.getTimeout());
			appCtxExecutor.setWatchdog(timer);
			
			appCtxExecutor.setTaskExecutor(sameThreadTaskExecutor);
			appCtxExecutor.setMonitoringCounter(contextsStarted);

			contextsStarted.increment();
		}
		else {
			// do nothing; by default contexts do not wait for services.
		}

		Runnable contextRefresh = new Runnable() {
			public void run() {
				context.refresh();
			}
		};

		// synch/asynch context creation
		try {
			if (config.isCreateAsynchronously()) {
				if (log.isDebugEnabled()) {
					log.debug("Asynchronous context creation for bundle "
							+ OsgiStringUtils.nullSafeNameAndSymName(bundle));
				}
				// for the asynch stuff use the executor
				taskExecutor.execute(contextRefresh);
			}
			else {
				if (log.isDebugEnabled()) {
					log.debug("Synchronous context creation for bundle "
							+ OsgiStringUtils.nullSafeNameAndSymName(bundle));
				}
				// for the synch stuff, use the queue
				syncTaskExecutor.execute(contextRefresh);
			}
		}
		catch (Throwable e) {
			log.warn("Cannot start bundle " + OsgiStringUtils.nullSafeSymbolicName(bundle) + " due to", e);
		}
	}

	protected OsgiBundleXmlApplicationContext createApplicationContext(BundleContext context, String[] locations) {
		OsgiBundleXmlApplicationContext sdoac = new OsgiBundleXmlApplicationContext(locations);
		sdoac.setBundleContext(context);
		return sdoac;
	}

	/**
	 * Closing an application context is a potentially long-running activity,
	 * however, we *have* to do it synchronously during the event process as the
	 * BundleContext object is not valid once we return from this method.
	 * 
	 * @param bundle
	 */
	protected void maybeCloseApplicationContextFor(Bundle bundle) {
		final ConfigurableOsgiBundleApplicationContext context = (ConfigurableOsgiBundleApplicationContext) managedContexts.remove(new Long(
				bundle.getBundleId()));
		if (context == null) {
			return;
		}

		RunnableTimedExecution.execute(new Runnable() {
			public void run() {
				context.close();
			}
		}, SHUTDOWN_WAIT_TIME);
	}

	/**
	 * If this bundle defines handler mapping or schema mapping resources, then
	 * register it with the namespace plugin handler.
	 * 
	 * @param bundle
	 */
	// TODO: should this be into Namespace plugins?
	// TODO: what about custom locations (outside of META-INF/spring)
	// FIXME: rely on OSGI-IO here
	protected void maybeAddNamespaceHandlerFor(Bundle bundle) {
		if (OsgiBundleUtils.isSystemBundle(bundle)) {
			return; // Do not resolve namespace and entity handlers from the
			// system bundle
		}
		if (isKnopflerfish) {
			// knopflerfish (2.0.0) has a bug #1581187 which gives a classcast
			// exception if you call getResource
			// from outside of the bundle, yet getResource works bettor on
			// equinox....
			// see
			// http://sourceforge.net/tracker/index.php?func=detail&aid=1581187&group_id=82798&atid=567241
			if (bundle.getEntry(SPRING_HANDLER_MAPPINGS_LOCATION) != null
					|| bundle.getEntry(PluggableSchemaResolver.DEFAULT_SCHEMA_MAPPINGS_LOCATION) != null) {
				addHandler(bundle);
			}
		}
		else {
			if (bundle.getResource(SPRING_HANDLER_MAPPINGS_LOCATION) != null
					|| bundle.getResource(PluggableSchemaResolver.DEFAULT_SCHEMA_MAPPINGS_LOCATION) != null) {
				addHandler(bundle);
			}
		}
	}

	/**
	 * Add this bundle to those known to provide handler or schema mappings
	 * 
	 * @param bundle
	 */
	protected void addHandler(Bundle bundle) {

		if (!ConfigUtils.matchExtenderVersionRange(bundle, extenderVersion)) {
			if (log.isDebugEnabled())
				log.debug("bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle)
						+ "] expects an extender w/ version["
						+ OsgiBundleUtils.getHeaderAsVersion(bundle, ConfigUtils.EXTENDER_VERSION)
						+ "] which does not match current extender w/ version[" + extenderVersion
						+ "]; skipping bundle from handler detection");
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug("Adding namespace handler resolver for " + bundle.getSymbolicName());
		}

		this.namespacePlugins.addHandler(bundle);
	}

	/**
	 * Remove this bundle from the set of those known to provide handler or
	 * schema mappings.
	 * 
	 * @param bundle
	 */
	protected void maybeRemoveNameSpaceHandlerFor(Bundle bundle) {
		Assert.notNull(namespacePlugins);
		boolean removed = this.namespacePlugins.removeHandler(bundle);
		if (removed && log.isDebugEnabled()) {
			log.debug("Removed namespace handler resolver for " + bundle.getSymbolicName());
		}
	}

	/**
	 * <p/> Create the task executor to be used for any asynchronous activity
	 * kicked off by this bundle. By default an
	 * <code>org.springframework.core.task.SimpleAsyncTaskExecutor</code> will
	 * be used. This should be sufficient for most purposes.
	 * </p>
	 * <p/> It is possible to configure the extender bundle to use an alternate
	 * task executor implementation (for example, a CommonJ WorkManager based
	 * implementation when running under WLS or WebSphere). To do this attach a
	 * fragment to the extender bundle that defines a Spring application context
	 * configuration file in META-INF/spring/extender.xml. If such a resource
	 * exists, then an application context will be created from that
	 * configuration file, and a bean named "taskExecutor" will be looked up by
	 * name. If such a bean exists, it will be used.
	 * </p>
	 * 
	 * @param context
	 * @return TaskExecutor
	 */
	// TODO: can we simplify this somewhat further so there is no need for a
	// different XML file
	protected TaskExecutor createTaskExecutor(BundleContext context) {
		Bundle extenderBundle = context.getBundle();
		URL extenderConfigFile = extenderBundle.getResource(EXTENDER_CONFIG_FILE_LOCATION);
		if (extenderConfigFile != null) {
			String[] locations = new String[] { extenderConfigFile.toExternalForm() };

			this.extenderContext = new OsgiBundleXmlApplicationContext(locations);
			this.extenderContext.setBundleContext(context);
			this.extenderContext.setNamespaceResolver(this.namespacePlugins);

			extenderContext.refresh();

			if (extenderContext.containsBean(TASK_EXECUTOR_BEAN_NAME)) {
				Object taskExecutor = extenderContext.getBean(TASK_EXECUTOR_BEAN_NAME);
				if (taskExecutor instanceof TaskExecutor) {
					return (TaskExecutor) taskExecutor;
				}
				else {
					if (log.isErrorEnabled()) {
						log.error("Bean 'taskExecutor' in META-INF/spring/extender.xml configuration file "
								+ "is not an instance of " + TaskExecutor.class.getName() + ". " + "Using defaults.");
					}
				}
			}
			else {
				if (log.isWarnEnabled()) {
					log.warn("Found META-INF/spring/extender.xml configuration file, but no bean "
							+ "named 'taskExecutor' was defined; using defaults.");
				}
			}
		}

		synchronized (monitor) {
			threadGroup = new ThreadGroup("spring-osgi-extender-task-executor");
			threadGroup.setDaemon(false);
		}

		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setThreadGroup(threadGroup);
		return taskExecutor;
	}

}
