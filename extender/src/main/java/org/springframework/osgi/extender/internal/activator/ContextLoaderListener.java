/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.extender.internal.activator;

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticaster;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;
import org.springframework.osgi.extender.internal.support.ExtenderConfiguration;
import org.springframework.osgi.extender.internal.support.NamespaceManager;
import org.springframework.osgi.extender.support.internal.ConfigUtils;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;
import org.springframework.osgi.service.importer.support.OsgiServiceCollectionProxyFactoryBean;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Osgi Extender that bootstraps 'Spring powered bundles'.
 * 
 * <p/> The class listens to bundle events and manages the creation and destruction of application contexts for bundles
 * that have one or both of: <ul> <li>A manifest header entry Spring-Context <li>XML files in META-INF/spring folder
 * </ul>
 * 
 * <p/> The extender also discovers any Spring namespace/schema handlers in resolved bundles and makes them available
 * through a dedicated OSGi service.
 * 
 * <p/> The extender behaviour can be customized by attaching fragments to the extender bundle. On startup, the extender
 * will look for <code>META-INF/spring/*.xml</code> files and merge them into an application context. From the resulting
 * context, the context will look for beans with predefined names to determine its configuration. The current version
 * recognises the following bean names:
 * 
 * <table border="1"> <tr> <th>Bean Name</th> <th>Bean Type</th> <th>Description</th> </tr> <tr>
 * <td><code>taskExecutor</code></td> <td><code>org.springframework.core.task.TaskExecutor</code></td> <td>Task executor
 * used for creating the discovered application contexts.</td> </tr> <tr> <td><code>shutdownTaskExecutor</code></td>
 * <td><code>org.springframework.core.task.TaskExecutor</code></td> <td>Task executor used for shutting down various
 * application contexts.</td> </tr> <tr> <td><code>extenderProperties</code></td>
 * <td><code>java.util.Properties</code></td> <td>Various properties for configuring the extender behaviour (see
 * below)</td> </tr> </table>
 * 
 * <p/> <code>extenderProperties</code> recognises the following properties:
 * 
 * <table border="1"> <tr> <th>Name</th> <th>Type</th> <th>Description</th> </tr> <tr>
 * <td><code>shutdown.wait.time</code></td> <td>Number</td> <td>The amount of time the extender will wait for each
 * application context to shutdown gracefully. Expressed in milliseconds.</td> </tr> <tr>
 * <td><code>process.annotations</code></td> <td>Boolean</td> <td>Whether or not, the extender will process SpringOSGi
 * annotations.</td> </tr> </table>
 * 
 * <p/> Note: The extender configuration context is created during the bundle activation (a synchronous OSGi lifecycle
 * callback) and should contain only simple bean definitions that will not delay context initialisation. </p>
 * 
 * @author Bill Gallagher
 * @author Andy Piper
 * @author Hal Hildebrand
 * @author Adrian Colyer
 * @author Costin Leau
 */
public class ContextLoaderListener implements BundleActivator {

	/**
	 * Common base class for {@link ContextLoaderListener} listeners.
	 * 
	 * @author Costin Leau
	 */
	private abstract class BaseListener implements SynchronousBundleListener {

		static final int LAZY_ACTIVATION_EVENT_TYPE = 0x00000200;

		/**
		 * common cache used for tracking down bundles started lazily so they don't get processed twice (once when
		 * started lazy, once when started fully)
		 */
		protected Map<Bundle, Object> lazyBundleCache = new WeakHashMap<Bundle, Object>();
		/** dummy value for the bundle cache */
		private final Object VALUE = new Object();

		// caches the bundle
		protected void push(Bundle bundle) {
			synchronized (lazyBundleCache) {
				lazyBundleCache.put(bundle, VALUE);
			}
		}

		// checks the presence of the bundle as well as removing it
		protected boolean pop(Bundle bundle) {
			synchronized (lazyBundleCache) {
				return (lazyBundleCache.remove(bundle) != null);
			}
		}

		/**
		 * A bundle has been started, stopped, resolved, or unresolved. This method is a synchronous callback, do not do
		 * any long-running work in this thread.
		 * 
		 * @see org.osgi.framework.SynchronousBundleListener#bundleChanged
		 */
		public void bundleChanged(BundleEvent event) {

			boolean trace = log.isTraceEnabled();

			// check if the listener is still alive
			if (isClosed) {
				if (trace)
					log.trace("Listener is closed; events are being ignored");
				return;
			}
			if (trace) {
				log.trace("Processing bundle event [" + OsgiStringUtils.nullSafeToString(event) + "] for bundle ["
						+ OsgiStringUtils.nullSafeSymbolicName(event.getBundle()) + "]");
			}
			try {
				handleEvent(event);
			} catch (Exception ex) {
				/* log exceptions before swallowing */
				log.warn("Got exception while handling event " + event, ex);
			}
		}

		protected abstract void handleEvent(BundleEvent event);
	}

	/**
	 * Bundle listener used for detecting namespace handler/resolvers. Exists as a separate listener so that it can be
	 * registered early to avoid race conditions with bundles in INSTALLING state but still to avoid premature context
	 * creation before the Spring {@link ContextLoaderListener} is not fully initialized.
	 * 
	 * @author Costin Leau
	 */
	private class NamespaceBundleLister extends BaseListener {

		protected void handleEvent(BundleEvent event) {

			Bundle bundle = event.getBundle();

			switch (event.getType()) {
			case LAZY_ACTIVATION_EVENT_TYPE: {
				push(bundle);
				maybeAddNamespaceHandlerFor(bundle, true);
			}
			case BundleEvent.STARTED: {
				if (!pop(bundle)) {
					maybeAddNamespaceHandlerFor(bundle, false);
				}
				break;
			}
			case BundleEvent.STOPPED: {
				pop(bundle);
				maybeRemoveNameSpaceHandlerFor(bundle);
				break;
			}
			default:
				break;
			}
		}
	}

	/**
	 * Bundle listener used for context creation/destruction.
	 */
	private class ContextBundleListener extends BaseListener {

		protected void handleEvent(BundleEvent event) {

			Bundle bundle = event.getBundle();

			// ignore current bundle for context creation
			if (bundle.getBundleId() == bundleId) {
				return;
			}

			switch (event.getType()) {
			case BundleEvent.STARTED: {
				lifecycleManager.maybeCreateApplicationContextFor(bundle);
				break;
			}
			case BundleEvent.STOPPING: {
				if (OsgiBundleUtils.isSystemBundle(bundle)) {
					if (log.isDebugEnabled()) {
						log.debug("System bundle stopping");
					}
					// System bundle is shutting down; Special handling for
					// framework shutdown
					shutdown();
				} else {
					lifecycleManager.maybeCloseApplicationContextFor(bundle);
				}
				break;
			}
			default:
				break;
			}
		}
	}

	private class RefreshEventPropagater implements
			OsgiBundleApplicationContextListener<OsgiBundleApplicationContextEvent> {

		public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
			if (event instanceof OsgiBundleContextRefreshedEvent) {
				if (log.isTraceEnabled()) {
					log.trace("Calling post-refresh on processor " + processor);
				}
				processor.postProcessRefresh((ConfigurableOsgiBundleApplicationContext) event.getApplicationContext());
			}
			if (event instanceof OsgiBundleContextFailedEvent) {
				if (log.isTraceEnabled()) {
					log.trace("Calling fail-refresh on processor " + processor);
				}
				OsgiBundleContextFailedEvent failureEvent = (OsgiBundleContextFailedEvent) event;
				processor.postProcessRefreshFailure(((ConfigurableOsgiBundleApplicationContext) event
						.getApplicationContext()), failureEvent.getFailureCause());
			}
		}
	}

	protected final Log log = LogFactory.getLog(getClass());

	/** extender bundle id */
	private long bundleId;

	/** extender configuration */
	private ExtenderConfiguration extenderConfiguration;

	/** Spring namespace/resolver manager */
	private NamespaceManager nsManager;

	/** The bundle's context */
	private BundleContext bundleContext;

	/** Bundle listener interested in context creation */
	private SynchronousBundleListener contextListener;

	/** Bundle listener interested in namespace resolvers/parsers discovery */
	private SynchronousBundleListener nsListener;

	/**
	 * Monitor used for dealing with the bundle activator and synchronous bundle threads
	 */
	private final Object monitor = new Object();

	/**
	 * flag indicating whether the context is down or not - useful during shutdown
	 */
	private volatile boolean isClosed = false;

	/** This extender version */
	private Version extenderVersion;

	private volatile OsgiBundleApplicationContextEventMulticaster multicaster;

	private volatile LifecycleManager lifecycleManager;
	private volatile VersionMatcher versionMatcher;
	private volatile OsgiContextProcessor processor;
	private volatile ListListenerAdapter osgiListeners;

	/**
	 * <p/> Called by OSGi when this bundle is started. Finds all previously resolved bundles and adds namespace
	 * handlers for them if necessary. </p> <p/> Creates application contexts for bundles started before the extender
	 * was started. </p> <p/> Registers a namespace/entity resolving service for use by web app contexts. </p>
	 * 
	 * @see org.osgi.framework.BundleActivator#start
	 */
	public void start(BundleContext context) throws Exception {

		this.bundleContext = context;
		this.bundleId = context.getBundle().getBundleId();

		this.extenderVersion = OsgiBundleUtils.getBundleVersion(context.getBundle());
		log.info("Starting [" + bundleContext.getBundle().getSymbolicName() + "] bundle v.[" + extenderVersion + "]");
		versionMatcher = new DefaultVersionMatcher(getManagedBundleExtenderVersionHeader(), extenderVersion);
		processor = createContextProcessor();

		// init cache (to prevent ad-hoc Java Bean discovery on lazy bundles)
		initJavaBeansCache();

		// Step 1 : discover existing namespaces (in case there are fragments with custom XML definitions)
		nsManager = new NamespaceManager(context);
		initNamespaceHandlers(bundleContext);

		// Step 2: initialize the extender configuration
		extenderConfiguration = initExtenderConfiguration(bundleContext);

		// init the OSGi event dispatch/listening system
		initListenerService();

		// initialize the configuration once namespace handlers have been detected
		lifecycleManager =
				new LifecycleManager(extenderConfiguration, versionMatcher, createContextConfigFactory(),
						this.processor, getTypeCompatibilityChecker(), bundleContext);

		// Step 3: discover the bundles that are started
		// and require context creation
		initStartedBundles(bundleContext);
	}

	protected ExtenderConfiguration initExtenderConfiguration(BundleContext bundleContext) {
		return new ExtenderConfiguration(bundleContext, log);
	}

	protected OsgiContextProcessor createContextProcessor() {
		return new NoOpOsgiContextProcessor();
	}

	protected TypeCompatibilityChecker getTypeCompatibilityChecker() {
		return null;
	}

	protected String getManagedBundleExtenderVersionHeader() {
		return ConfigUtils.EXTENDER_VERSION;
	}

	protected void initNamespaceHandlers(BundleContext context) {
		nsManager = new NamespaceManager(context);

		// register listener first to make sure any bundles in INSTALLED state
		// are not lost
		nsListener = new NamespaceBundleLister();
		context.addBundleListener(nsListener);

		Bundle[] previousBundles = context.getBundles();

		for (int i = 0; i < previousBundles.length; i++) {
			Bundle bundle = previousBundles[i];
			if (OsgiBundleUtils.isBundleActive(bundle)) {
				maybeAddNamespaceHandlerFor(bundle, false);
			} else if (OsgiBundleUtils.isBundleLazyActivated(bundle)) {
				maybeAddNamespaceHandlerFor(bundle, true);
			}
		}

		// discovery finished, publish the resolvers/parsers in the OSGi space
		nsManager.afterPropertiesSet();
	}

	protected void initStartedBundles(BundleContext bundleContext) {
		// register the context creation listener
		contextListener = new ContextBundleListener();
		// listen to any changes in bundles
		bundleContext.addBundleListener(contextListener);
		// get the bundles again to get an updated view
		Bundle[] previousBundles = bundleContext.getBundles();

		// Instantiate all previously resolved bundles which are Spring
		// powered
		for (int i = 0; i < previousBundles.length; i++) {
			if (OsgiBundleUtils.isBundleActive(previousBundles[i])) {
				try {
					lifecycleManager.maybeCreateApplicationContextFor(previousBundles[i]);
				} catch (Throwable e) {
					log.warn("Cannot start bundle " + OsgiStringUtils.nullSafeSymbolicName(previousBundles[i])
							+ " due to", e);
				}
			}
		}
	}

	/**
	 * Called by OSGi when this bundled is stopped. Unregister the namespace/entity resolving service and clear all
	 * state. No further management of application contexts created by this extender prior to stopping the bundle occurs
	 * after this point (even if the extender bundle is subsequently restarted).
	 * 
	 * @see org.osgi.framework.BundleActivator#stop
	 */
	public void stop(BundleContext context) throws Exception {
		shutdown();
	}

	/**
	 * Shutdown the extender and all bundled managed by it. Shutdown of contexts is in the topological order of the
	 * dependency graph formed by the service references.
	 */
	protected void shutdown() {
		synchronized (monitor) {
			// if already closed, bail out
			if (isClosed)
				return;
			else
				isClosed = true;
		}
		log.info("Stopping [" + bundleContext.getBundle().getSymbolicName() + "] bundle v.[" + extenderVersion + "]");

		destroyJavaBeansCache();

		// remove the bundle listeners (we are closing down)
		if (contextListener != null) {
			bundleContext.removeBundleListener(contextListener);
			contextListener = null;
		}

		if (nsListener != null) {
			bundleContext.removeBundleListener(nsListener);
			nsListener = null;
		}

		// close managed bundles
		lifecycleManager.destroy();
		// clear the namespace registry
		nsManager.destroy();

		// release multicaster
		if (multicaster != null) {
			multicaster.removeAllListeners();
			multicaster = null;
		}
		// release listeners
		osgiListeners.destroy();
		osgiListeners = null;

		extenderConfiguration.destroy();
	}

	private void initJavaBeansCache() {
		Class<?>[] classes =
				new Class<?>[] { OsgiServiceFactoryBean.class, OsgiServiceProxyFactoryBean.class,
						OsgiServiceCollectionProxyFactoryBean.class };

		CachedIntrospectionResults.acceptClassLoader(OsgiStringUtils.class.getClassLoader());

		for (Class<?> clazz : classes) {
			BeanUtils.getPropertyDescriptors(clazz);
		}
	}

	private void destroyJavaBeansCache() {
		CachedIntrospectionResults.clearClassLoader(OsgiStringUtils.class.getClassLoader());
	}

	protected void maybeAddNamespaceHandlerFor(Bundle bundle, boolean isLazy) {
		if (handlerBundleMatchesExtenderVersion(bundle))
			nsManager.maybeAddNamespaceHandlerFor(bundle, isLazy);
	}

	protected void maybeRemoveNameSpaceHandlerFor(Bundle bundle) {
		if (handlerBundleMatchesExtenderVersion(bundle))
			nsManager.maybeRemoveNameSpaceHandlerFor(bundle);
	}

	/**
	 * Utility method that does extender range versioning and approapriate
	 * 
	 * logging.
	 * 
	 * @param bundle
	 */
	private boolean handlerBundleMatchesExtenderVersion(Bundle bundle) {
		if (!versionMatcher.matchVersion(bundle)) {
			if (log.isDebugEnabled())
				log.debug("Ignoring handler bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle)
						+ "] due to mismatch in expected extender version");
			return false;
		}
		return true;
	}

	protected ApplicationContextConfigurationFactory createContextConfigFactory() {
		return new DefaultApplicationContextConfigurationFactory();
	}

	protected void initListenerService() {
		multicaster = extenderConfiguration.getEventMulticaster();

		multicaster.addApplicationListener(new RefreshEventPropagater());
		addApplicationListener(multicaster);
		multicaster.addApplicationListener(extenderConfiguration.getContextEventListener());

		if (log.isDebugEnabled())
			log.debug("Initialization of OSGi listeners service completed...");
	}

	protected void addApplicationListener(OsgiBundleApplicationContextEventMulticaster multicaster) {
		osgiListeners = new ListListenerAdapter(bundleContext);
		osgiListeners.afterPropertiesSet();
		// register the listener that does the dispatching
		multicaster.addApplicationListener(osgiListeners);
	}
}