package org.springframework.osgi.extender.internal.dependencies.startup;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.osgi.context.DelegatedExecutionOsgiBundleApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.extender.OsgiServiceDependencyFactory;
import org.springframework.osgi.extender.event.BootstrappingDependenciesEvent;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.extender.internal.util.PrivilegedUtils;
import org.springframework.osgi.service.importer.OsgiServiceDependency;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitEndedEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitStartingEvent;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.osgi.util.OsgiListenerUtils;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * ServiceListener used for tracking dependent services. Even if the ServiceListener receives event synchronously,
 * mutable properties should be synchronized to guarantee safe publishing between threads.
 * 
 * @author Costin Leau
 * @author Hal Hildebrand
 * @author Andy Piper
 */
public class DependencyServiceManager {

	private static final Log log = LogFactory.getLog(DependencyServiceManager.class);

	protected final Map<MandatoryServiceDependency, String> dependencies =
			Collections.synchronizedMap(new LinkedHashMap<MandatoryServiceDependency, String>());

	protected final Map<MandatoryServiceDependency, String> unsatisfiedDependencies =
			Collections.synchronizedMap(new LinkedHashMap<MandatoryServiceDependency, String>());

	private final ContextExecutorAccessor contextStateAccessor;

	private final BundleContext bundleContext;

	private final ServiceListener listener;

	private final DelegatedExecutionOsgiBundleApplicationContext context;

	/**
	 * Task to execute if all dependencies are met.
	 */
	private final Runnable executeIfDone;

	/** Maximum waiting time used in events when waiting for dependencies */
	private final long waitTime;

	/** dependency factories */
	private List<OsgiServiceDependencyFactory> dependencyFactories;

	/**
	 * Actual ServiceListener.
	 * 
	 * @author Costin Leau
	 * @author Hal Hildebrand
	 */
	private class DependencyServiceListener implements ServiceListener {

		/**
		 * Process serviceChanged events, completing context initialization if all the required dependencies are
		 * satisfied.
		 * 
		 * @param serviceEvent
		 */
		public void serviceChanged(ServiceEvent serviceEvent) {
			boolean trace = log.isTraceEnabled();

			try {
				if (unsatisfiedDependencies.isEmpty()) {

					// already completed but likely called due to threading
					if (trace) {
						log.trace("Handling service event, but no unsatisfied dependencies exist for "
								+ context.getDisplayName());
					}

					return;
				}

				ServiceReference ref = serviceEvent.getServiceReference();
				if (trace) {
					log.trace("Handling service event [" + OsgiStringUtils.nullSafeToString(serviceEvent) + ":"
							+ OsgiStringUtils.nullSafeToString(ref) + "] for " + context.getDisplayName());
				}

				updateDependencies(serviceEvent);

				ContextState state = contextStateAccessor.getContextState();

				// already resolved (closed or timed-out)
				if (state.isResolved()) {
					deregister();
					return;
				}

				// Good to go!
				if (unsatisfiedDependencies.isEmpty()) {
					deregister();
					// context.listener = null;
					log.info("No unsatisfied OSGi service dependencies; completing initialization for "
							+ context.getDisplayName());

					// execute task to complete initialization
					// NOTE: the runnable should be able to delegate any long
					// process to a
					// different thread.
					executeIfDone.run();
				}
			} catch (Throwable th) {
				// frameworks will simply not log exception for event handlers
				log.error("Exception during dependency processing for " + context.getDisplayName(), th);
				contextStateAccessor.fail(th);
			}
		}

		private void updateDependencies(ServiceEvent serviceEvent) {
			boolean trace = log.isTraceEnabled();
			boolean debug = log.isDebugEnabled();

			String referenceToString = null;
			String contextToString = null;

			if (debug) {
				referenceToString = OsgiStringUtils.nullSafeToString(serviceEvent.getServiceReference());
				contextToString = context.getDisplayName();
			}

			for (MandatoryServiceDependency dependency : dependencies.keySet()) {
				// check all dependencies (there might be multiple imports for the same service)
				if (dependency.matches(serviceEvent)) {
					if (trace) {
						log.trace(dependency + " matched: " + referenceToString);
					}

					switch (serviceEvent.getType()) {

					case ServiceEvent.REGISTERED:
					case ServiceEvent.MODIFIED:
						dependency.increment();
						if (unsatisfiedDependencies.remove(dependency) != null) {
							if (debug) {
								log.debug("Registered dependency for " + contextToString + "; eliminating "
										+ dependency + ", remaining [" + unsatisfiedDependencies + "]");
							}

							sendDependencySatisfiedEvent(dependency);
							sendBootstrappingDependenciesEvent(unsatisfiedDependencies.keySet());
						} else {
							if (debug) {
								log.debug("Increasing the number of matching services for " + contextToString + "; "
										+ dependency + ", remaining [" + unsatisfiedDependencies + "]");
							}
						}

						break;

					case ServiceEvent.UNREGISTERING:
						int count = dependency.decrement();
						if (count == 0) {
							unsatisfiedDependencies.put(dependency, dependency.getBeanName());
							if (debug) {
								log.debug("Unregistered dependency for " + contextToString + " adding " + dependency
										+ "; total unsatisfied [" + unsatisfiedDependencies + "]");
							}

							sendDependencyUnsatisfiedEvent(dependency);
							sendBootstrappingDependenciesEvent(unsatisfiedDependencies.keySet());
						} else {
							if (debug) {
								log.debug("Decreasing the number of matching services for " + contextToString + "; "
										+ dependency + " still has " + count + " matches left");
							}
						}
						break;
					default: // do nothing
						if (debug) {
							log.debug("Unknown service event type for: " + dependency);
						}
						break;
					}
				} else {
					if (trace) {
						log.trace(dependency + " does not match: " + referenceToString);
					}
				}
			}
		}
	}

	/**
	 * Create a dependency manager, indicating the executor bound to, the context that contains the dependencies and the
	 * task to execute if all dependencies are met.
	 * 
	 * @param executor
	 * @param context
	 * @param executeIfDone
	 */
	public DependencyServiceManager(ContextExecutorAccessor executor,
			DelegatedExecutionOsgiBundleApplicationContext context,
			List<OsgiServiceDependencyFactory> dependencyFactories, Runnable executeIfDone, long maxWaitTime) {
		this.contextStateAccessor = executor;
		this.context = context;
		this.dependencyFactories = new ArrayList<OsgiServiceDependencyFactory>(8);

		if (dependencyFactories != null)
			this.dependencyFactories.addAll(dependencyFactories);

		this.waitTime = maxWaitTime;
		this.bundleContext = context.getBundleContext();
		this.listener = new DependencyServiceListener();

		this.executeIfDone = executeIfDone;
	}

	protected void findServiceDependencies() throws Exception {
		try {
			if (System.getSecurityManager() != null) {
				final AccessControlContext acc = getAcc();

				PrivilegedUtils.executeWithCustomTCCL(context.getClassLoader(),
						new PrivilegedUtils.UnprivilegedThrowableExecution<Object>() {
							public Object run() throws Throwable {
								AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
									public Object run() throws Exception {
										doFindDependencies();
										return null;
									}
								}, acc);
								return null;
							}
						});
			} else {
				doFindDependencies();
			}
		} catch (Throwable th) {
			if (th instanceof Exception)
				throw ((Exception) th);
			throw (Error) th;
		}

		if (log.isDebugEnabled()) {
			log.debug(dependencies.size() + " OSGi service dependencies, " + unsatisfiedDependencies.size()
					+ " unsatisfied (for beans " + unsatisfiedDependencies.values() + ") in "
					+ context.getDisplayName());
		}

		if (!unsatisfiedDependencies.isEmpty()) {
			log.info(context.getDisplayName() + " is waiting for unsatisfied dependencies ["
					+ unsatisfiedDependencies.values() + "]");
		}
		if (log.isTraceEnabled()) {
			log.trace("Total OSGi service dependencies beans " + dependencies.values());
			log.trace("Unsatified OSGi service dependencies beans " + unsatisfiedDependencies.values());
		}
	}

	private void doFindDependencies() throws Exception {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		boolean debug = log.isDebugEnabled();
		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("Looking for dependency factories inside bean factory [" + beanFactory.toString() + "]");

		Map<String, OsgiServiceDependencyFactory> localFactories =
				BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, OsgiServiceDependencyFactory.class, true,
						false);

		if (trace)
			log.trace("Discovered local dependency factories: " + localFactories.keySet());

		dependencyFactories.addAll(localFactories.values());

		for (Iterator<OsgiServiceDependencyFactory> iterator = dependencyFactories.iterator(); iterator.hasNext();) {
			OsgiServiceDependencyFactory dependencyFactory = iterator.next();
			Collection<OsgiServiceDependency> discoveredDependencies = null;

			if (trace) {
				log.trace("Interogating dependency factory " + dependencyFactory);
			}
			try {
				discoveredDependencies = dependencyFactory.getServiceDependencies(bundleContext, beanFactory);
			} catch (Exception ex) {
				log.warn("Dependency factory " + dependencyFactory
						+ " threw exception while detecting dependencies for beanFactory " + beanFactory + " in "
						+ context.getDisplayName(), ex);
				throw ex;
			}
			// add the dependencies one by one
			if (discoveredDependencies != null)
				for (OsgiServiceDependency dependency : discoveredDependencies) {
					if (dependency.isMandatory()) {
						MandatoryServiceDependency msd = new MandatoryServiceDependency(bundleContext, dependency);
						dependencies.put(msd, dependency.getBeanName());

						if (!msd.isServicePresent()) {
							log.info("Adding OSGi service dependency for importer [" + msd.getBeanName()
									+ "] matching OSGi filter [" + msd.filterAsString + "]");
							unsatisfiedDependencies.put(msd, dependency.getBeanName());
						} else {
							if (debug)
								log.debug("OSGi service dependency for importer [" + msd.getBeanName()
										+ "] is already satisfied");
						}
					}
				}
		}
	}

	protected boolean isSatisfied() {
		return unsatisfiedDependencies.isEmpty();
	}

	public Map<MandatoryServiceDependency, String> getUnsatisfiedDependencies() {
		return unsatisfiedDependencies;
	}

	protected void register() {
		final String filter = createDependencyFilter();
		if (log.isDebugEnabled()) {
			log.debug(context.getDisplayName() + " has registered service dependency dependencyDetector with filter: "
					+ filter);
		}

		// send dependency event before registering the filter
		sendInitialBootstrappingEvents(unsatisfiedDependencies.keySet());

		if (System.getSecurityManager() != null) {
			AccessControlContext acc = getAcc();
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					OsgiListenerUtils.addServiceListener(bundleContext, listener, filter);
					return null;
				}
			}, acc);
		} else {
			OsgiListenerUtils.addServiceListener(bundleContext, listener, filter);
		}
	}

	/**
	 * Look at all dependencies and create an appropriate filter. This method concatenates the filters into one. Note
	 * that not just unsatisfied dependencies are considered since their number can grow.
	 * 
	 * @return
	 */
	private String createDependencyFilter() {
		return createDependencyFilter(dependencies.keySet());
	}

	String createUnsatisfiedDependencyFilter() {
		return createDependencyFilter(unsatisfiedDependencies.keySet());
	}

	private String createDependencyFilter(Collection<MandatoryServiceDependency> dependencies) {
		if (dependencies.isEmpty()) {
			return null;
		}

		boolean multiple = dependencies.size() > 1;
		StringBuilder sb = new StringBuilder(dependencies.size() << 7);
		if (multiple) {
			sb.append("(|");
		}
		for (MandatoryServiceDependency dependency : dependencies) {
			sb.append(dependency.filterAsString);
		}
		if (multiple) {
			sb.append(')');
		}

		String filter = sb.toString();

		return filter;
	}

	protected void deregister() {
		if (log.isDebugEnabled()) {
			log.debug("Deregistering service dependency dependencyDetector for " + context.getDisplayName());
		}

		OsgiListenerUtils.removeServiceListener(bundleContext, listener);
	}

	List<OsgiServiceDependencyEvent> getUnsatisfiedDependenciesAsEvents() {
		return getUnsatisfiedDependenciesAsEvents(unsatisfiedDependencies.keySet());
	}

	private List<OsgiServiceDependencyEvent> getUnsatisfiedDependenciesAsEvents(
			Collection<MandatoryServiceDependency> deps) {
		List<OsgiServiceDependencyEvent> dependencies = new ArrayList<OsgiServiceDependencyEvent>(deps.size());

		for (MandatoryServiceDependency entry : deps) {
			OsgiServiceDependencyEvent nestedEvent =
					new OsgiServiceDependencyWaitStartingEvent(context, entry.getServiceDependency(), waitTime);
			dependencies.add(nestedEvent);
		}

		return Collections.unmodifiableList(dependencies);

	}

	// event notification
	private void sendDependencyUnsatisfiedEvent(MandatoryServiceDependency dependency) {
		OsgiServiceDependencyEvent nestedEvent =
				new OsgiServiceDependencyWaitStartingEvent(context, dependency.getServiceDependency(), waitTime);
		BootstrappingDependencyEvent dependencyEvent =
				new BootstrappingDependencyEvent(context, context.getBundle(), nestedEvent);
		publishEvent(dependencyEvent);
	}

	private void sendDependencySatisfiedEvent(MandatoryServiceDependency dependency) {
		OsgiServiceDependencyEvent nestedEvent =
				new OsgiServiceDependencyWaitEndedEvent(context, dependency.getServiceDependency(), waitTime);
		BootstrappingDependencyEvent dependencyEvent =
				new BootstrappingDependencyEvent(context, context.getBundle(), nestedEvent);
		publishEvent(dependencyEvent);
	}

	private void sendInitialBootstrappingEvents(Set<MandatoryServiceDependency> deps) {
		// send the fine grained event
		List<OsgiServiceDependencyEvent> events = getUnsatisfiedDependenciesAsEvents(deps);
		for (OsgiServiceDependencyEvent nestedEvent : events) {
			BootstrappingDependencyEvent dependencyEvent =
					new BootstrappingDependencyEvent(context, context.getBundle(), nestedEvent);
			publishEvent(dependencyEvent);
		}

		// followed by the composite one
		String filterAsString = createDependencyFilter(deps);
		Filter filter = (filterAsString != null ? OsgiFilterUtils.createFilter(filterAsString) : null);
		BootstrappingDependenciesEvent event =
				new BootstrappingDependenciesEvent(context, context.getBundle(), events, filter, waitTime);

		publishEvent(event);
	}

	private void sendBootstrappingDependenciesEvent(Set<MandatoryServiceDependency> deps) {
		List<OsgiServiceDependencyEvent> events = getUnsatisfiedDependenciesAsEvents(deps);
		String filterAsString = createDependencyFilter(deps);
		Filter filter = (filterAsString != null ? OsgiFilterUtils.createFilter(filterAsString) : null);
		BootstrappingDependenciesEvent event =
				new BootstrappingDependenciesEvent(context, context.getBundle(), events, filter, waitTime);

		publishEvent(event);
	}

	private void publishEvent(OsgiBundleApplicationContextEvent dependencyEvent) {
		this.contextStateAccessor.getEventMulticaster().multicastEvent(dependencyEvent);
	}

	private AccessControlContext getAcc() {
		AutowireCapableBeanFactory beanFactory = context.getAutowireCapableBeanFactory();
		if (beanFactory instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) beanFactory).getAccessControlContext();
		}
		return null;
	}
}