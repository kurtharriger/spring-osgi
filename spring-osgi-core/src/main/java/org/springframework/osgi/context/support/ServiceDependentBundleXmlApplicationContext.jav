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
package org.springframework.osgi.context.support;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.osgi.service.CardinalityOptions;
import org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.osgi.util.OsgiListenerUtils;

/**
 * An AbstractBundleXmlApplicationContext which delays initialization until all
 * of its osgi:reference targets are complete. <p/>
 * 
 * @author Andy Piper
 * @author Hal Hildebrand
 * @author Costin Leau
 */
public class ServiceDependentBundleXmlApplicationContext extends AbstractBundleXmlApplicationContext {

	/**
	 * ServiceListener used for tracking dependent services. As the
	 * ServiceListener receives event synchronously there is no need for
	 * synchronization.
	 * 
	 * @author Costin Leau
	 */
	private class DependencyListener implements ServiceListener {

		/**
		 * Process serviceChanged events, completing context initialization if
		 * all the required dependencies are satisfied.
		 * 
		 * @param serviceEvent
		 */
		public void serviceChanged(ServiceEvent serviceEvent) {
			if (unsatisfiedDependencies.isEmpty()) { // already completed.
				return;
			}

			for (Iterator i = dependencies.iterator(); i.hasNext();) {
				Dependency dependency = (Dependency) i.next();
				if (dependency.matches(serviceEvent)) {
					switch (serviceEvent.getType()) {
					case ServiceEvent.MODIFIED:
					case ServiceEvent.REGISTERED:
						unsatisfiedDependencies.remove(dependency);
						if (log.isDebugEnabled())
							log.debug("found service; eliminating dependency =" + dependency);
						break;
					case ServiceEvent.UNREGISTERING:
						unsatisfiedDependencies.add(dependency);
						if (log.isDebugEnabled())
							log.debug("service unregistered; adding dependency =" + dependency);
						break;
					default: // do nothing
						break;
					}
				}
			}
			// Good to go!
			if (unsatisfiedDependencies.isEmpty()) {
				if (log.isDebugEnabled()) {
					log.debug("Services satisfied, completing initialization for " + getDisplayName());
				}
				completeContextInitialization();
			}
			else {
				registerListener(); // re-register with the new filter
			}
		}
	}

	private class Dependency {
		private final Filter filter;

		private final String clazz;

		private final int cardinality;

		private Dependency(Filter filter, String clazz, int cardinality) {
			this.filter = filter;
			this.clazz = clazz;
			this.cardinality = cardinality;
		}

		public boolean matches(ServiceEvent event) {
			return filter.match(event.getServiceReference());
		}

		public boolean isSatisfied() {
			ServiceReference[] refs;
			try {
				refs = getBundleContext().getServiceReferences(clazz, filter.toString());
			}
			catch (InvalidSyntaxException e) {
				throw (IllegalStateException) new IllegalStateException("Filter string '" + filter.toString()
						+ "' has invalid syntax: " + e.getMessage()).initCause(e);
			}
			return !CardinalityOptions.atLeastOneRequired(cardinality) || (refs != null && refs.length != 0);
		}

		public String toString() {
			return "Dependency on [" + filter + "]";
		}

		/**
		 * @return Returns the filter.
		 */
		public Filter getFilter() {
			return filter;
		}

	}


	private static final Log log = LogFactory.getLog(ServiceDependentBundleXmlApplicationContext.class);

	private Set dependencies = new LinkedHashSet();

	private Set unsatisfiedDependencies = new LinkedHashSet();

	private ClassLoader savedCcl;

	private ServiceListener dependencyTracker;

	public ServiceDependentBundleXmlApplicationContext(BundleContext context, String[] configLocations) {
		super(context, configLocations);

		savedCcl = Thread.currentThread().getContextClassLoader();

		dependencyTracker = new DependencyListener();

		refresh();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.support.AbstractApplicationContext#onRefresh()
	 */
	protected void onRefresh() throws BeansException {
		dependencies.clear();
		unsatisfiedDependencies.clear();

		// Listen for services so that we can determine when we are ready for
		// activation.
		findServiceDependencies();

		OsgiListenerUtils.addServiceListener(getBundleContext(), dependencyTracker, (String) null);

		if (!hasUnsatisifiedServiceDependencies()) {
			if (log.isDebugEnabled()) {
				log.debug("Services already satisfied, completing initialization for " + getDisplayName());
			}

			getBundleContext().removeServiceListener(dependencyTracker);
			super.onRefresh();
		}
	}

	protected void doClose() {
		try {
			getBundleContext().removeServiceListener(dependencyTracker);
		}
		catch (IllegalStateException e) {
			logger.warn("exception thrown while removing service listener " + e);
		}
	}

	/**
	 * Complete the initialization of this context now that we know all services
	 * are available.
	 */
	protected void completeContextInitialization() {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		BundleContext bc = LocalBundleContext.getContext();
		try {
			Thread.currentThread().setContextClassLoader(savedCcl);
			LocalBundleContext.setContext(getBundleContext());
			dependencies.clear();
			getBundleContext().removeServiceListener(dependencyTracker);
			// Build the bean definitions.
			super.refresh();
			publishContextAsOsgiServiceIfNecessary();

		}
		catch (RuntimeException ex) {
			if (log.isErrorEnabled()) {
				log.error("Unable to complete application context initializition for '" + getBundle().getSymbolicName()
						+ "'", ex);
			}
			throw ex;
		}
		finally {
			LocalBundleContext.setContext(bc);
			Thread.currentThread().setContextClassLoader(ccl);
		}
	}

	protected void initializeUnsatisfiedServiceDependencies() {
		for (Iterator i = dependencies.iterator(); i.hasNext();) {
			Dependency dependency = (Dependency) i.next();
			if (!dependency.isSatisfied()) {
				unsatisfiedDependencies.add(dependency);
			}
		}
	}

	private boolean hasUnsatisifiedServiceDependencies() {
		return unsatisfiedDependencies.isEmpty();
	}

	private void registerListener() {
		boolean multiple = unsatisfiedDependencies.size() > 1;
		StringBuffer sb = new StringBuffer(100 * unsatisfiedDependencies.size());
		if (multiple) {
			sb.append("(|");
		}
		for (Iterator i = unsatisfiedDependencies.iterator(); i.hasNext();) {
			((Dependency) i.next()).appendTo(sb);
		}
		if (multiple) {
			sb.append(')');
		}
		String filter = sb.toString();
		if (log.isInfoEnabled()) {
			log.info(getDisplayName() + " registering filter: " + filter);
		}
		try {
			getBundleContext().addServiceListener(dependencyTracker, filter);
		}
		catch (InvalidSyntaxException e) {
			throw (IllegalStateException) new IllegalStateException("Filter string '" + filter
					+ "' has invalid syntax: " + e.getMessage()).initCause(e);
		}
	}

	public boolean isAvailable() {
		return isActive() && unsatisfiedDependencies.isEmpty();
	}

}
