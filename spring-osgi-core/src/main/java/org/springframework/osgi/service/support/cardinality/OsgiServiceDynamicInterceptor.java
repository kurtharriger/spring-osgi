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
package org.springframework.osgi.service.support.cardinality;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.service.OsgiServiceUtils;
import org.springframework.osgi.service.ServiceUnavailableException;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.osgi.service.support.DefaultRetryCallback;
import org.springframework.osgi.service.support.RetryTemplate;
import org.springframework.osgi.service.support.ServiceWrapper;

/**
 * Interceptor adding dynamic behavior for unary service (..1 cardinality). It
 * will look for a service using the given class and filter name, retrying if
 * the service is down or unavailable. Will dynamically rebound a new service,
 * if one is available with a higher service ranking.
 * 
 * <strong>Note</strong>: this is a stateful interceptor and should not be
 * shared.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceDynamicInterceptor extends OsgiServiceClassLoaderInvoker implements InitializingBean {

	private ServiceWrapper wrapper;

	protected RetryTemplate retryTemplate = new RetryTemplate();

	protected String clazz;

	protected String filter;

	private TargetSourceLifecycleListener[] listeners = new TargetSourceLifecycleListener[0];

	public OsgiServiceDynamicInterceptor(BundleContext context, int contextClassLoader) {
		super(context, null, contextClassLoader);
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
				if (updateWrapperIfNecessary(ref, serviceId, ranking)) {
					// inform listeners
					callListenersBind(ref);
				}

				break;
			case (ServiceEvent.MODIFIED):
				// same as ServiceEvent.REGISTERED
				if (updateWrapperIfNecessary(ref, serviceId, ranking)) {
					// inform listeners
					callListenersBind(ref);
				}

				break;
			case (ServiceEvent.UNREGISTERING):
				boolean updated = false;

				synchronized (OsgiServiceDynamicInterceptor.class) {
					// remove service
					if (wrapper != null) {
						if (serviceId == wrapper.getServiceId()) {
							updated = true;
							wrapper.cleanup();
							wrapper = null;
						}
					}
				}

				if (updated)
					callListenersUnbind(ref);
				try {
					ServiceReference refs[] = context.getServiceReferences(clazz, filter);

					// FIXME: place the discovery process into one class
					// quick hack: just pick the first one
					if (refs != null && refs.length > 0) {
						serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, refs[0]));
					}
				}
				catch (InvalidSyntaxException ise) {
					throw new IllegalArgumentException("invalid filter");
				}
				
				break;
			default:
				throw new IllegalArgumentException("unsupported event type");
			}
		}

		private void callListenersBind(ServiceReference reference) {
			boolean debug = log.isDebugEnabled();
			for (int i = 0; i < listeners.length; i++) {
				if (debug)
					log.debug("calling bind on " + listeners[i] + " w/ reference " + reference);
				listeners[i].bind(null, context.getService(reference));
			}
		}

		private void callListenersUnbind(ServiceReference reference) {
			boolean debug = log.isDebugEnabled();

			for (int i = 0; i < listeners.length; i++) {
				if (debug)
					log.debug("calling unbind on " + listeners[i] + " w/ reference " + reference);
				listeners[i].unbind(null, context.getService(reference));
			}
		}

		private boolean updateWrapperIfNecessary(ServiceReference ref, long serviceId, int serviceRanking) {

			boolean updated = false;
			synchronized (OsgiServiceDynamicInterceptor.class) {
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

				return updated;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.support.cardinality.OsgiServiceInvoker#getTarget()
	 */
	protected Object getTarget() throws Throwable {
		Object target = null;

		if (wrapper == null || !wrapper.isServiceAlive())
			lookupService();

		target = (wrapper != null ? wrapper.getService() : null);

		// nothing found
		if (target == null) {
			throw new ServiceUnavailableException("could not find service", null, null);
		}

		return target;
	}

	// TODO: do nothing since we catch the events anyway
	protected ServiceWrapper lookupService() {
		return (ServiceWrapper) retryTemplate.execute(new DefaultRetryCallback() {
			public Object doWithRetry() {
				// ServiceReference ref = context.getServiceReference(clazz);
				// return (ref != null ? new ServiceWrapper(ref, context) :
				// null);
				return null;
			}
		});
	}

	public void afterPropertiesSet() {
		addListener(context, filter);

	}

	/**
	 * Add the service listener to context and register also already
	 * @param context
	 * @param filter
	 */
	private void addListener(BundleContext context, String filter) {
		try {
			ServiceListener listener = new Listener();
			// add listener
			context.addServiceListener(listener, filter);

			// now get the already registered services and call the listener
			// (the listener can handle duplicates)
			ServiceReference[] alreadyRegistered = context.getServiceReferences(clazz, filter);
			if (alreadyRegistered != null)
				for (int i = 0; i < alreadyRegistered.length; i++) {
					listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, alreadyRegistered[i]));
				}
		}
		catch (InvalidSyntaxException isex) {
			throw (RuntimeException) new IllegalArgumentException("invalid filter").initCause(isex);
		}
	}

	public void setClass(String clazz) {
		this.clazz = clazz;
	}

	public void setFilter(String filter) {
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
