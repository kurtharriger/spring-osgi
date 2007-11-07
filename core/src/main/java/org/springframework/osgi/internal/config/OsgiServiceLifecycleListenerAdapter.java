/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.internal.config;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.internal.util.ReflectionUtils;
import org.springframework.osgi.service.importer.OsgiServiceLifecycleListener;
import org.springframework.osgi.service.importer.ServiceReferenceAccessor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * OsgiServiceLifecycleListener wrapper for custom beans, useful when custom
 * methods are being used.
 * 
 * @author Costin Leau
 * 
 */
class OsgiServiceLifecycleListenerAdapter implements OsgiServiceLifecycleListener, InitializingBean {

	private static final Log log = LogFactory.getLog(OsgiServiceLifecycleListenerAdapter.class);

	/**
	 * Map of methods keyed by the first parameter which indicates the service
	 * type expected.
	 */
	private Map bindMethods, unbindMethods;

	/**
	 * anyName(ServiceReference reference) method signature.
	 */
	private Method bindReference, unbindReference;

	private String bindMethod, unbindMethod;

	private final Object target;

	private final boolean isLifecycleListener;

	OsgiServiceLifecycleListenerAdapter(Object object) {
		Assert.notNull(object);
		this.target = object;
		isLifecycleListener = target instanceof OsgiServiceLifecycleListener;
	}

	/**
	 * Initialise adapter. Determine custom methods and do validation.
	 */
	public void afterPropertiesSet() {
		Class clazz = target.getClass();
		
		if (isLifecycleListener)
			if (log.isDebugEnabled())
				log.debug(clazz.getName() + " is a lifecycle listener");

		bindMethods = CustomListenerAdapterUtils.determineCustomMethods(clazz, bindMethod);
		unbindMethods = CustomListenerAdapterUtils.determineCustomMethods(clazz, unbindMethod);

		if (StringUtils.hasText(bindMethod)) {
			// determine methods using ServiceReference signature
			bindReference = org.springframework.util.ReflectionUtils.findMethod(clazz, bindMethod,
				new Class[] { ServiceReference.class });

			if (bindReference != null)
				org.springframework.util.ReflectionUtils.makeAccessible(bindReference);
		}
		if (StringUtils.hasText(unbindMethod)) {
			unbindReference = org.springframework.util.ReflectionUtils.findMethod(clazz, unbindMethod,
				new Class[] { ServiceReference.class });

			if (unbindReference != null)
				org.springframework.util.ReflectionUtils.makeAccessible(unbindReference);
		}

		if (!isLifecycleListener
				&& (bindMethods.isEmpty() && unbindMethods.isEmpty() && bindReference == null && unbindReference == null))
			throw new IllegalArgumentException("target object needs to implement "
					+ OsgiServiceLifecycleListener.class.getName()
					+ " or custom bind/unbind methods have to be specified");
	}

	/**
	 * Invoke method with signature bla(ServiceReference ref).
	 * 
	 * @param target
	 * @param method
	 * @param service
	 */
	private void invokeCustomServiceReferenceMethod(Object target, Method method, Object service) {
		if (method != null) {
			boolean trace = log.isTraceEnabled();

			// get the service reference
			// find the compatible types (accept null service)
			if (trace)
				log.trace("invoking listener custom method " + method);

			ServiceReference ref = ((ServiceReferenceAccessor) service).getServiceReference();

			try {
				ReflectionUtils.invokeMethod(method, target, new Object[] { ref });
			}
			// make sure to log exceptions and continue with the
			// rest of
			// the listeners
			catch (Exception ex) {
				Exception cause = ReflectionUtils.getInvocationException(ex);
				log.warn("custom method [" + method + "] threw exception when passing service reference ["
						+ (service != null ? service.getClass().getName() : null) + "]", cause);
			}
		}
	}

	public void bind(Object service, Map properties) throws Exception {
		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("invoking bind method for service " + service + " with props=" + properties);

		// first call interface method (if it exists)
		if (isLifecycleListener) {
			if (trace)
				log.trace("invoking listener interface methods");

			try {
				((OsgiServiceLifecycleListener) target).bind(service, properties);
			}
			catch (Exception ex) {
				log.warn("standard bind method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}

		CustomListenerAdapterUtils.invokeCustomMethods(target, bindMethods, service, properties);
		invokeCustomServiceReferenceMethod(target, bindReference, service);
	}

	public void unbind(Object service, Map properties) throws Exception {
		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("invoking unbind method for service " + service + " with props=" + properties);

		// first call interface method (if it exists)
		if (isLifecycleListener) {
			if (trace)
				log.trace("invoking listener interface methods");
			try {
				((OsgiServiceLifecycleListener) target).unbind(service, properties);
			}
			catch (Exception ex) {
				log.warn("standard unbind method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}

		CustomListenerAdapterUtils.invokeCustomMethods(target, unbindMethods, service, properties);
		invokeCustomServiceReferenceMethod(target, unbindReference, service);
	}

	/**
	 * @param bindMethod The bindMethod to set.
	 */
	public void setBindMethod(String bindMethod) {
		this.bindMethod = bindMethod;
	}

	/**
	 * @param unbindMethod The unbindMethod to set.
	 */
	public void setUnbindMethod(String unbindMethod) {
		this.unbindMethod = unbindMethod;
	}
}
