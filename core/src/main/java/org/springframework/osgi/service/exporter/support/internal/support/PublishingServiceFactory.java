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
 */

package org.springframework.osgi.service.exporter.support.internal.support;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.osgi.service.util.internal.aop.ProxyUtils;
import org.springframework.osgi.service.util.internal.aop.ServiceTCCLInterceptor;
import org.springframework.osgi.util.DebugUtils;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * ServiceFactory used for publishing the service beans. Acts as a a wrapper around special beans (such as
 * ServiceFactory). Additionally, used to create TCCL managing proxies.
 * 
 * @author Costin Leau
 */
public class PublishingServiceFactory implements ServiceFactory {

	/** logger */
	private static final Log log = LogFactory.getLog(PublishingServiceFactory.class);

	/** proxy cache in case the given bean has a non-singleton scope */
	private final Map<Object, WeakReference<Object>> proxyCache;

	private final LazyTargetResolver targetResolver;
	private final Class<?>[] classes;
	private final boolean createTCCLProxy;
	private final ClassLoader classLoader;
	private final ClassLoader aopClassLoader;
	private final BundleContext bundleContext;
	private final Object lock = new Object();

	/**
	 * Constructs a new <code>PublishingServiceFactory</code> instance. Since its an internal class, this constructor
	 * accepts a number of parameters to sacrifice readability for thread-safety.
	 * 
	 * @param classes
	 * @param target
	 * @param beanFactory
	 * @param targetBeanName
	 * @param createTCCLProxy
	 * @param classLoader
	 * @param aopClassLoader
	 * @param bundleContext
	 */
	public PublishingServiceFactory(LazyTargetResolver targetResolver, Class<?>[] classes, boolean createTCCLProxy,
			ClassLoader classLoader, ClassLoader aopClassLoader, BundleContext bundleContext) {
		super();

		this.targetResolver = targetResolver;
		this.classes = classes;

		this.createTCCLProxy = createTCCLProxy;
		this.classLoader = classLoader;
		this.aopClassLoader = aopClassLoader;
		this.bundleContext = bundleContext;

		proxyCache = (createTCCLProxy ? new WeakHashMap<Object, WeakReference<Object>>(4) : null);
	}

	public Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {
		if (log.isTraceEnabled()) {
			log.trace("Get service called by bundle " + OsgiStringUtils.nullSafeName(bundle) + " on registration "
					+ OsgiStringUtils.nullSafeToString(serviceRegistration.getReference()));
		}

		targetResolver.activate();
		Object bn = targetResolver.getBean();

		// handle SF beans
		if (bn instanceof ServiceFactory) {
			bn = ((ServiceFactory) bn).getService(bundle, serviceRegistration);
		}

		if (createTCCLProxy) {
			// check proxy cache
			synchronized (proxyCache) {
				WeakReference<Object> value = proxyCache.get(bn);
				Object proxy = null;
				if (value != null) {
					proxy = value.get();
				}
				if (proxy == null) {
					proxy = createCLLProxy(bn);
					proxyCache.put(bn, new WeakReference<Object>(proxy));
				}
				bn = proxy;
			}
		}

		return bn;
	}

	/**
	 * Proxy the target object with an interceptor that manages the context classloader. This should be applied only if
	 * such management is needed. Additionally, this method uses a cache to prevent multiple proxies to be created for
	 * the same object.
	 * 
	 * @param target
	 * @return
	 */
	private Object createCLLProxy(final Object target) {
		try {
			return ProxyUtils.createProxy(classes, target, aopClassLoader, bundleContext,
					new Advice[] { new ServiceTCCLInterceptor(classLoader) });
		} catch (Throwable th) {
			log.error("Cannot create TCCL managed proxy; falling back to the naked object", th);
			if (th instanceof NoClassDefFoundError) {
				NoClassDefFoundError ncdfe = (NoClassDefFoundError) th;
				if (log.isWarnEnabled()) {
					DebugUtils.debugClassLoadingThrowable(ncdfe, bundleContext.getBundle(), classes);
				}
				throw ncdfe;
			}
		}

		return target;
	}

	public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object service) {

		if (log.isTraceEnabled()) {
			log.trace("Unget service called by bundle " + OsgiStringUtils.nullSafeName(bundle) + " on registration "
					+ OsgiStringUtils.nullSafeToString(serviceRegistration.getReference()));
		}

		Class<?> type = targetResolver.getType();
		// handle SF beans
		if (ServiceFactory.class.isAssignableFrom(type)) {
			ServiceFactory sf = (ServiceFactory) targetResolver.getBean();
			sf.ungetService(bundle, serviceRegistration, service);
		}

		if (createTCCLProxy) {
			synchronized (proxyCache) {
				// trigger purging of unused entries
				proxyCache.size();
			}
		}
	}
}