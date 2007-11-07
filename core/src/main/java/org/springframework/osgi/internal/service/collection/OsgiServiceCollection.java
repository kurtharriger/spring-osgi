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
package org.springframework.osgi.internal.service.collection;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.support.LocalBundleContext;
import org.springframework.osgi.internal.service.ImporterProxy;
import org.springframework.osgi.internal.service.interceptor.LocalBundleContextAdvice;
import org.springframework.osgi.internal.service.interceptor.OsgiServiceStaticInterceptor;
import org.springframework.osgi.internal.service.interceptor.ServiceReferenceAwareAdvice;
import org.springframework.osgi.internal.service.util.OsgiServiceBindingUtils;
import org.springframework.osgi.internal.util.ClassUtils;
import org.springframework.osgi.service.ServiceUnavailableException;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.osgi.service.importer.ReferenceClassLoadingOptions;
import org.springframework.osgi.util.OsgiListenerUtils;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * OSGi service dynamic collection - allows iterating while the underlying
 * storage is being shrunk/expanded. This collection is read-only - its content
 * is being retrieved dynamically from the OSGi platform.
 * 
 * <p/> This collection and its iterators are thread-safe. That is, multiple
 * threads can access the collection. However, since the collection is
 * read-only, it cannot be modified by the client.
 * 
 * @see Collection
 * @author Costin Leau
 */
public class OsgiServiceCollection implements Collection, InitializingBean, ImporterProxy {

	/**
	 * Listener tracking the OSGi services which form the dynamic collection.
	 * 
	 * @author Costin Leau
	 */
	private class Listener implements ServiceListener {

		public void serviceChanged(ServiceEvent event) {
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();

			try {
				Thread.currentThread().setContextClassLoader(classLoader);
				ServiceReference ref = event.getServiceReference();
				Long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
				boolean collectionModified = false;

				switch (event.getType()) {

				case (ServiceEvent.REGISTERED):
				case (ServiceEvent.MODIFIED):
					// same as ServiceEvent.REGISTERED
					synchronized (serviceProxies) {
						if (!servicesIdMap.containsKey(serviceId)) {
							Object proxy = createServiceProxy(ref);
							// let the dynamic collection decide if the service
							// is added or not (think set, sorted set)
							if (serviceProxies.add(proxy)) {
								collectionModified = true;
								servicesIdMap.put(serviceId, proxy);
							}
						}
					}
					// inform listeners
					// TODO: should this be part of the lock also?
					if (collectionModified)
						OsgiServiceBindingUtils.callListenersBind(context, ref, listeners);

					break;
				case (ServiceEvent.UNREGISTERING):
					synchronized (serviceProxies) {
						// remove service id / proxy association
						Object proxy = servicesIdMap.remove(serviceId);
						if (proxy != null) {
							// before removal, allow analysis
							checkDeadProxies(proxy);
							// remove service proxy
							collectionModified = serviceProxies.remove(proxy);
							// invalidate it
							invalidateProxy(proxy);
						}
					}
					// TODO: should this be part of the lock also?
					if (collectionModified)
						OsgiServiceBindingUtils.callListenersUnbind(context, ref, listeners);
					break;

				default:
					throw new IllegalArgumentException("unsupported event type:" + event);
				}
			}
			// OSGi swallows these exceptions so make sure we get a chance to
			// see them.
			catch (Throwable re) {
				if (log.isWarnEnabled()) {
					log.warn("serviceChanged() processing failed", re);
				}
			}
			finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}
		}
	}

	/**
	 * Read-only iterator wrapper around the dynamic collection iterator.
	 * 
	 * @author Costin Leau
	 * 
	 */
	protected class OsgiServiceIterator implements Iterator {
		// dynamic thread-safe iterator
		private final Iterator iter = serviceProxies.iterator();

		public boolean hasNext() {
			mandatoryServiceCheck();
			return iter.hasNext();
		}

		public Object next() {
			synchronized (serviceProxies) {
				mandatoryServiceCheck();
				Object proxy = iter.next();
				return (proxy == null ? tailDeadProxy : proxy);
			}
		}

		public void remove() {
			// write operations disabled
			throw new UnsupportedOperationException();
		}
	}

	private static final Log log = LogFactory.getLog(OsgiServiceCollection.class);

	// map of services
	// the service id is used as key while the service proxy is used for
	// values
	// Map<ServiceId, ImporterProxy>
	// 
	// NOTE: this collection is protected by the 'serviceProxies' lock.
	protected final Map servicesIdMap = new LinkedHashMap(8);

	/**
	 * The dynamic collection.
	 */
	protected DynamicCollection serviceProxies;

	/**
	 * Recall the last proxy for the rare case, where a service goes down
	 * between the #hasNext() and #next() call of an iterator.
	 * 
	 * Subclasses should implement their own strategy when it comes to assign a
	 * value to it through
	 */
	protected volatile Object tailDeadProxy;

	private final Filter filter;

	private final BundleContext context;

	private int contextClassLoader = ReferenceClassLoadingOptions.CLIENT.shortValue();

	protected final ClassLoader classLoader;

	// advices to be applied when creating service proxy
	private Object[] interceptors = new Object[0];

	private TargetSourceLifecycleListener[] listeners = new TargetSourceLifecycleListener[0];

	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	private Class[] interfaces;

	protected final boolean atLeastOneServiceMandatory;

	private final LocalBundleContextAdvice bundleContextInterceptor;

	public OsgiServiceCollection(Filter filter, BundleContext context, ClassLoader classLoader, boolean mandatory) {
		Assert.notNull(classLoader, "ClassLoader is required");
		Assert.notNull(context, "context is required");

		this.filter = filter;
		this.context = context;
		this.classLoader = classLoader;

		this.atLeastOneServiceMandatory = mandatory;

		// share instance
		bundleContextInterceptor = new LocalBundleContextAdvice(context);
	}

	public void afterPropertiesSet() {
		// create service proxies collection
		this.serviceProxies = createInternalDynamicStorage();

		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("adding osgi listener for services matching [" + filter + "]");
		OsgiListenerUtils.addServiceListener(context, new Listener(), filter);

		if (atLeastOneServiceMandatory) {
			if (trace)
				log.trace("1..x cardinality - looking for service [" + filter + "] at startup...");
			mandatoryServiceCheck();
		}
	}

	/**
	 * Check to see wether at least one service is available.
	 */
	protected void mandatoryServiceCheck() {
		if (atLeastOneServiceMandatory && serviceProxies.isEmpty())
			throw new ServiceUnavailableException(filter);
	}

	/**
	 * Create the dynamic storage used internally. The storage <strong>has</strong>
	 * to be thread-safe.
	 */
	protected DynamicCollection createInternalDynamicStorage() {
		return new DynamicCollection();
	}

	/**
	 * Create a service proxy over the service reference. The proxy purpose is
	 * to transparently decouple the client from holding a strong reference to
	 * the service (which might go away).
	 * 
	 * This method will create a proxy based on the service reference
	 * {@link Constants#OBJECTCLASS} exposed classes.
	 * 
	 * @param ref
	 */
	protected Object createServiceProxy(ServiceReference ref) {

		ProxyFactory factory = new ProxyFactory();

		Class[] classes = discoverProxyClasses(ref);
		if (log.isDebugEnabled())
			log.debug("generating 'greedy' service proxy using classes " + ObjectUtils.nullSafeToString(classes)
					+ " over " + ObjectUtils.nullSafeToString(interfaces));

		ClassUtils.configureFactoryForClass(factory, classes);

		// add mixin first
		factory.addAdvice(new ServiceReferenceAwareAdvice(ref));
		// add bundle context interceptor
		factory.addAdvice(bundleContextInterceptor);

		// FIXME: add tccl handling
		// share stateless interceptors
		// factory.addAdvice(invoker);

		// add the interceptors
		// FIXME: why allow custom interceptors to be specified (?)
		if (this.interceptors != null) {
			for (int i = 0; i < this.interceptors.length; i++) {
				factory.addAdvisor(this.advisorAdapterRegistry.wrap(this.interceptors[i]));
			}
		}
		// add the invoker interceptor last
		factory.addAdvice(new OsgiServiceStaticInterceptor(context, ref));

		// TODO: why not add these?
		// factory.setOptimize(true);
		// factory.setFrozen(true);

		return factory.getProxy(classLoader);
	}

	protected Class[] discoverProxyClasses(ServiceReference ref) {
		String[] classNames = OsgiServiceReferenceUtils.getServiceObjectClasses(ref);

		// try to get as many interfaces as possible
		Class[] classes = ClassUtils.loadClasses(classNames, classLoader);
		// exclude final classes
		classes = ClassUtils.excludeClassesWithModifier(classes, Modifier.FINAL);
		// remove class duplicates/parents
		classes = ClassUtils.removeParents(classes);

		return classes;
	}

	private void invalidateProxy(Object proxy) {
		// TODO: add proxy invalidation
	}

	/**
	 * Hook for tracking the last disappearing service to cope with the rare
	 * case, where the last service in the collection disappears between calls
	 * to hasNext() and next() on an iterator at the end of the collection.
	 * 
	 * @param proxy
	 */
	protected void checkDeadProxies(Object proxy, int proxyCollectionPos) {
		if (proxyCollectionPos == serviceProxies.size() - 1)
			tailDeadProxy = proxy;
	}

	/**
	 * Private method, computing the index and share it with subclasses.
	 * @param proxy
	 */
	private void checkDeadProxies(Object proxy) {
		// no need for a collection lock (alreayd have it)
		int index = serviceProxies.indexOf(proxy);
		checkDeadProxies(proxy, index);
	}

	public Iterator iterator() {
		return new OsgiServiceIterator();
	}

	public int size() {
		mandatoryServiceCheck();
		return serviceProxies.size();
	}

	public String toString() {
		mandatoryServiceCheck();
		synchronized (serviceProxies) {
			return serviceProxies.toString();
		}
	}

	public boolean isSatisfied() {
		if (atLeastOneServiceMandatory)
			return serviceProxies.isEmpty();
		else
			return true;
	}

	//
	// write operations forbidden
	//
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean add(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Object o) {
		mandatoryServiceCheck();
		return serviceProxies.contains(o);
	}

	public boolean containsAll(Collection c) {
		mandatoryServiceCheck();
		return serviceProxies.containsAll(c);
	}

	public boolean isEmpty() {
		mandatoryServiceCheck();
		return size() == 0;
	}

	public Object[] toArray() {
		mandatoryServiceCheck();
		return serviceProxies.toArray();
	}

	public Object[] toArray(Object[] array) {
		mandatoryServiceCheck();
		return serviceProxies.toArray(array);
	}

	/**
	 * @return Returns the interceptors.
	 */
	public Object[] getInterceptors() {
		return interceptors;
	}

	/**
	 * The optional interceptors used when proxying each service. These will
	 * always be added before the OsgiServiceStaticInterceptor.
	 * 
	 * @param interceptors The interceptors to set.
	 */
	public void setInterceptors(Object[] interceptors) {
		Assert.notNull(interceptors, "argument should not be null");
		this.interceptors = interceptors;
	}

	/**
	 * @param listeners The listeners to set.
	 */
	public void setListeners(TargetSourceLifecycleListener[] listeners) {
		Assert.notNull(listeners, "argument should not be null");
		this.listeners = listeners;
	}

	/**
	 * @param contextClassLoader The contextClassLoader to set.
	 */
	public void setContextClassLoader(int contextClassLoader) {
		this.contextClassLoader = contextClassLoader;
	}

	public void setInterfaces(Class[] interfaces) {
		this.interfaces = interfaces;
	}
}
