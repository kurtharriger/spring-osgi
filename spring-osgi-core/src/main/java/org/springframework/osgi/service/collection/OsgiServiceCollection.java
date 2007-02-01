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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.support.ClassTargetSource;
import org.springframework.osgi.service.support.ServiceWrapper;
import org.springframework.osgi.service.support.cardinality.OsgiServiceStaticInterceptor;

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
public class OsgiServiceCollection implements Collection {

	/**
	 * Listener tracking the OSGi services
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class Listener implements ServiceListener {

		public void serviceChanged(ServiceEvent event) {
			ServiceReference ref = event.getServiceReference();
			Long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);

			switch (event.getType()) {

			case (ServiceEvent.REGISTERED):
				// be cautious!

				// add only if needed to not create a new service proxy for
				// nothing
				synchronized (serviceReferences) {
					if (!serviceReferences.containsKey(serviceId)) {
						serviceReferences.put(serviceId, createServiceProxy(ref));
						serviceIDs.add(serviceId);
					}
				}
				break;
			case (ServiceEvent.MODIFIED):
				// same as ServiceEvent.REGISTERED
				synchronized (serviceReferences) {
					if (!serviceReferences.containsKey(serviceId)) {
						serviceReferences.put(serviceId, createServiceProxy(ref));
						serviceIDs.add(serviceId);
					}
				}
				break;
			case (ServiceEvent.UNREGISTERING):
				synchronized (serviceReferences) {
					// remove servce
					Object proxy = serviceReferences.remove(serviceId);
					invalidateProxy(proxy);
					serviceIDs.remove(serviceId);
				}
				break;

			default:
				throw new IllegalArgumentException("unsupported event type");
			}
		}

	}

	// map of services
	// the service id is used for lookup while the service wrapper is used for
	// values
	protected final Map serviceReferences = CollectionFactory.createLinkedMapIfPossible(8);

	/** list binding the service IDs to the map of service proxies * */
	protected final Collection serviceIDs = createInternalDynamicStorage();

	private final String clazz;

	private final String filter;

	private final BundleContext context;

	public OsgiServiceCollection(String clazz, String filter, BundleContext context) {
		this.clazz = clazz;
		this.filter = filter;
		this.context = context;

		// TODO: add lazy behavior (register the listener only if the
		// collection is actually used)
		// add listener using the filter
		addListener(context, filter);
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
				throw new IllegalArgumentException("cannot create proxy", cnfex);
			}
		}

		ProxyFactory factory = new ProxyFactory();
		if (!intfs.isEmpty())
			factory.setInterfaces((Class[]) intfs.toArray(new Class[intfs.size()]));

		if (proxyClass != null) {
			factory.setProxyTargetClass(true);
			factory.setTargetSource(new ClassTargetSource(proxyClass));
		}

		factory.addAdvice(new OsgiServiceStaticInterceptor(new ServiceWrapper(ref, context)));
		// TODO: why not add these?
		// factory.setOptimize(true);
		// factory.setFrozen(true);

		return factory.getProxy(BundleDelegatingClassLoader.createBundleClassLoaderFor(ref.getBundle(),
				ProxyFactory.class.getClassLoader()));
	}

	private void invalidateProxy(Object proxy) {
		// TODO: add proxy invalidation
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
			ServiceReference[] alreadyRegistered = context.getServiceReferences(null, filter);
			for (int i = 0; i < alreadyRegistered.length; i++) {
				listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, alreadyRegistered[i]));
			}
		}
		catch (InvalidSyntaxException isex) {
			throw new IllegalArgumentException("invalid filter", isex);
		}
	}

	public Iterator iterator() {
		// use the service map not just the map of indexes
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

}
