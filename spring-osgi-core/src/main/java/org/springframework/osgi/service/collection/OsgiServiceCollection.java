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
package org.springframework.osgi.service.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
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
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.osgi.service.importer.ReferenceClassLoadingOptions;
import org.springframework.osgi.service.interceptor.OsgiServiceStaticInterceptor;
import org.springframework.osgi.service.util.OsgiServiceBindingUtils;
import org.springframework.osgi.util.OsgiListenerUtils;
import org.springframework.util.Assert;

/**
 * OSGi service dynamic collection - allows iterating while the underlying
 * storage is being shrunk/expanded. This collection is read-only - its content
 * is being retrieved dynamically from the OSGi platform.
 * 
 * <strong>Note</strong>:It is <strong>not</strong> synchronized.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceCollection implements Collection, InitializingBean {

	/**
	 * Listener tracking the OSGi services which form the dynamic collection.
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class Listener implements ServiceListener {

		public void serviceChanged(ServiceEvent event) {
			ServiceReference ref = event.getServiceReference();
			Long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
			boolean found = false;

			switch (event.getType()) {

			case (ServiceEvent.REGISTERED):
			case (ServiceEvent.MODIFIED):
				// same as ServiceEvent.REGISTERED
				synchronized (serviceReferences) {
					if (!serviceReferences.containsKey(serviceId)) {
						found = true;
						serviceReferences.put(serviceId, createServiceProxy(ref));
						serviceIDs.add(serviceId);
					}
				}
				// inform listeners
				if (found)
					OsgiServiceBindingUtils.callListenersBind(context, ref, listeners);

				break;
			case (ServiceEvent.UNREGISTERING):
				synchronized (serviceReferences) {
					// remove servce
					Object proxy = serviceReferences.remove(serviceId);
					found = serviceIDs.remove(serviceId);
					if (proxy != null) {
						invalidateProxy(proxy);
					}
				}

				if (found)
					OsgiServiceBindingUtils.callListenersUnbind(context, ref, listeners);
				break;

			default:
				throw new IllegalArgumentException("unsupported event type");
			}
		}
	}

	private static final Log log = LogFactory.getLog(OsgiServiceCollection.class);

	// map of services
	// the service id is used for lookup while the service wrapper is used for
	// values
	protected final Map serviceReferences = new LinkedHashMap(8);

	/** list binding the service IDs to the map of service proxies * */
	protected final Collection serviceIDs = createInternalDynamicStorage();

	private final Filter filter;

	private final BundleContext context;

	private int contextClassLoader = ReferenceClassLoadingOptions.CLIENT;

	// advices to be applied when creating service proxy
	private Object[] interceptors = new Object[0];

	private TargetSourceLifecycleListener[] listeners = new TargetSourceLifecycleListener[0];

	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	public OsgiServiceCollection(Filter filter, BundleContext context) {
		Assert.notNull(context, "context is required");

		this.filter = filter;
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {
		if (log.isDebugEnabled())
			log.debug("adding osgi listener for services matching [" + filter + "]");
		OsgiListenerUtils.addServiceListener(context, new Listener(), filter);
	}

	/**
	 * Create the dynamic storage used internally.
	 * 
	 * @return
	 */
	protected Collection createInternalDynamicStorage() {
		return new DynamicCollection();
	}

	/**
	 * Create a service proxy over the service reference. The proxy purpose is
	 * to transparently decouple the client from holding a strong reference to
	 * the service (which might go away).
	 * 
	 * @param ref
	 * @return
	 */
	private Object createServiceProxy(ServiceReference ref) {
		// get classes under which the service was registered
		String[] classes = (String[]) ref.getProperty(Constants.OBJECTCLASS);

		List intfs = new ArrayList();
		Class proxyClass = null;

		for (int i = 0; i < classes.length; i++) {
			// resolve classes (using the proper classloader)
			Bundle loader = ref.getBundle();
			try {
				Class clazz = loader.loadClass(classes[i]);
				// FIXME: discover lower class if multiple class names are used
				// (basically detect the lowest subclass)
				if (clazz.isInterface())
					intfs.add(clazz);
				else {
					proxyClass = clazz;
				}
			}
			catch (ClassNotFoundException cnfex) {
				throw (RuntimeException) new IllegalArgumentException("cannot create proxy").initCause(cnfex);
			}
		}

		ProxyFactory factory = new ProxyFactory();
		if (!intfs.isEmpty())
			factory.setInterfaces((Class[]) intfs.toArray(new Class[intfs.size()]));

		if (proxyClass != null) {
			factory.setProxyTargetClass(true);
			factory.setTargetClass(proxyClass);
		}

		// add the interceptors
		if (this.interceptors != null) {
			for (int i = 0; i < this.interceptors.length; i++) {
				factory.addAdvisor(this.advisorAdapterRegistry.wrap(this.interceptors[i]));
			}
		}

		factory.addAdvice(new OsgiServiceStaticInterceptor(context, ref, contextClassLoader));
		// TODO: why not add these?
		// factory.setOptimize(true);
		// factory.setFrozen(true);

		return factory.getProxy(BundleDelegatingClassLoader.createBundleClassLoaderFor(ref.getBundle(),
			ProxyFactory.class.getClassLoader()));
	}

	private void invalidateProxy(Object proxy) {
		// TODO: add proxy invalidation
	}

	public Iterator iterator() {
		// use the service map not just the list of indexes
		return new Iterator() {
			// dynamic iterator
			private final Iterator iter = serviceIDs.iterator();

			public boolean hasNext() {
				return iter.hasNext();
			}

			public Object next() {
				// extract the service proxy from the map
				return serviceReferences.get(iter.next());
			}

			public void remove() {
				// write operations disabled
				throw new UnsupportedOperationException();
			}
		};
	}

	public int size() {
		return serviceIDs.size();
	}

	public String toString() {
		return serviceReferences.values().toString();
	}

	// write operations forbidden
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
		return serviceReferences.containsValue(o);
	}

	public boolean containsAll(Collection c) {
		return serviceReferences.values().containsAll(c);
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public Object[] toArray() {
		return serviceReferences.values().toArray();
	}

	public Object[] toArray(Object[] array) {
		return serviceReferences.values().toArray(array);
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
}
