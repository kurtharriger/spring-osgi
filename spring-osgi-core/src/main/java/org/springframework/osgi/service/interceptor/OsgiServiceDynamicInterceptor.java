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
 */
package org.springframework.osgi.service.interceptor;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.ServiceUnavailableException;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.osgi.service.support.DefaultRetryCallback;
import org.springframework.osgi.service.support.RetryTemplate;
import org.springframework.osgi.service.support.ServiceWrapper;
import org.springframework.osgi.service.util.OsgiServiceBindingUtils;
import org.springframework.osgi.util.OsgiListenerUtils;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;

/**
 * Interceptor adding dynamic behavior for unary service (..1 cardinality). It
 * will look for a service using the given filter, retrying if the service is
 * down or unavailable. Will dynamically rebound a new service, if one is
 * available with a higher service ranking.
 * 
 * <p/>
 * 
 * <strong>Note</strong>: this is a stateful interceptor and should not be
 * shared.
 * 
 * @author Costin Leau
 */
public class OsgiServiceDynamicInterceptor extends OsgiServiceClassLoaderInvoker implements InitializingBean {

	private ServiceWrapper wrapper;

	protected RetryTemplate retryTemplate = new RetryTemplate();

	protected Filter filter;

	private TargetSourceLifecycleListener[] listeners = new TargetSourceLifecycleListener[0];

	private final boolean serviceRequiredAtStartup;

	public OsgiServiceDynamicInterceptor(BundleContext context, int contextClassLoader) {
		this(context, contextClassLoader, true);
	}

	public OsgiServiceDynamicInterceptor(BundleContext context, int contextClassLoader, boolean serviceRequiredAtStartup) {
		super(context, null, contextClassLoader);
		this.serviceRequiredAtStartup = serviceRequiredAtStartup;
	}

	private class Listener implements ServiceListener {

		public void serviceChanged(ServiceEvent event) {
			ServiceReference ref = event.getServiceReference();

			// service id
			long serviceId = ((Long) ref.getProperty(Constants.SERVICE_ID)).longValue();
			// service ranking
			Integer rank = (Integer) ref.getProperty(Constants.SERVICE_RANKING);
			int ranking = (rank == null ? 0 : rank.intValue());

			switch (event.getType()) {

			case (ServiceEvent.REGISTERED):
			case (ServiceEvent.MODIFIED):
				// same as ServiceEvent.REGISTERED
				if (updateWrapperIfNecessary(ref, serviceId, ranking)) {
					// inform listeners
					OsgiServiceBindingUtils.callListenersBind(context, ref, listeners);
				}

				break;
			case (ServiceEvent.UNREGISTERING):
				boolean updated = false;

				synchronized (OsgiServiceDynamicInterceptor.this) {
					// remove service
					if (wrapper != null) {
						if (serviceId == wrapper.getServiceId()) {
							updated = true;
							wrapper.cleanup();
							wrapper = null;
						}
					}
				}

				if (log.isDebugEnabled()) {
					String message = "service reference [" + ref + "] was unregistered";
					if (updated)
						message += " and was unbound from the service proxy";
					else
						message += " but did not affect the service proxy";
					log.debug(message);
				}

				if (updated)
					OsgiServiceBindingUtils.callListenersUnbind(context, ref, listeners);

				// discover the new reference
				ServiceReference newReference = OsgiServiceReferenceUtils.getServiceReference(context,
					filter.toString());

				if (newReference != null)
					// update the listeners
					serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, newReference));

				break;
			default:
				throw new IllegalArgumentException("unsupported event type");
			}
		}

		private boolean updateWrapperIfNecessary(ServiceReference ref, long serviceId, int serviceRanking) {
			boolean updated = false;
			try {
				synchronized (OsgiServiceDynamicInterceptor.this) {
					if (wrapper != null) {
						// we have a new service
						if (serviceRanking > wrapper.getServiceRanking()) {
							updated = true;
							wrapper = new ServiceWrapper(ref, context);
						}
						// if equality, use the service id
						if (serviceRanking == wrapper.getServiceRanking()) {
							if (serviceId < wrapper.getServiceId()) {
								updated = true;
								wrapper = new ServiceWrapper(ref, context);
							}
						}
					}
					else {
						wrapper = new ServiceWrapper(ref, context);
						updated = true;
					}
					OsgiServiceDynamicInterceptor.this.notifyAll();
					return updated;
				}
			}
			finally {
				if (log.isDebugEnabled()) {
					String message = "service reference [" + ref + "]";
					if (updated)
						message += " bound to proxy";
					else
						message += " not bound to proxy";
					log.debug(message);
				}

			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.interceptor.OsgiServiceInvoker#getTarget()
	 */
	protected Object getTarget() {
		Object target = null;

		if (wrapper == null || !wrapper.isServiceAlive())
			lookupService();

		target = (wrapper != null ? wrapper.getService() : null);

		// nothing found
		if (target == null) {
			throw new ServiceUnavailableException("Could not find service [" + wrapper + "], filter ["
					+ filter.toString() + "]", null, filter.toString());
		}

		return target;
	}

	protected synchronized ServiceWrapper lookupService() {
		return (ServiceWrapper) retryTemplate.execute(new DefaultRetryCallback() {
			public Object doWithRetry() {
				return (wrapper != null && wrapper.isServiceAlive()) ? wrapper : null;
			}
		}, this);
	}

	public void afterPropertiesSet() {
		boolean debug = log.isDebugEnabled();
		if (debug)
			log.debug("adding osgi listener for services matching [" + filter + "]");
		OsgiListenerUtils.addServiceListener(context, new Listener(), filter);
		if (serviceRequiredAtStartup) {
			if (debug)
				log.debug("1..x cardinality - looking for service at startup...");
			Object target = getTarget();
			if (debug)
				log.debug("service retrieved " + target);
		}
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	public RetryTemplate getRetryTemplate() {
		return retryTemplate;
	}

	public TargetSourceLifecycleListener[] getListeners() {
		return listeners;
	}

	public void setListeners(TargetSourceLifecycleListener[] listeners) {
		this.listeners = listeners;
	}

}
