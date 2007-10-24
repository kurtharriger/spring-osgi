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
package org.springframework.osgi.internal.service.exporter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.internal.config.TargetSourceLifecycleListenerWrapper;
import org.springframework.osgi.service.OsgiServiceRegistrationListener;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Adapter/wrapper class that handles listener with custom method invocation.
 * Similar in functionality to {@link TargetSourceLifecycleListenerWrapper}.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceRegistrationListenerWrapper implements OsgiServiceRegistrationListener, InitializingBean {

	private static final Log log = LogFactory.getLog(OsgiServiceRegistrationListenerWrapper.class);

	private final Object target;

	private final boolean isListener;

	private String registrationMethod, unregistrationMethod;

	/**
	 * Map of methods keyed by the first parameter which indicates the service
	 * type expected.
	 */
	private Map registrationMethods, unregistrationMethods;

	public OsgiServiceRegistrationListenerWrapper(Object object) {
		this.target = object;
		isListener = target instanceof OsgiServiceRegistrationListener;
	}

	/**
	 * Initialise adapter. Determine custom methods and do validation.
	 */
	public void afterPropertiesSet() {

		if (isListener)
			if (log.isDebugEnabled())
				log.debug(target.getClass().getName() + " is a registration listener");
		registrationMethods = determineCustomMethods(registrationMethod);
		unregistrationMethods = determineCustomMethods(unregistrationMethod);

		if (!isListener && (registrationMethods.isEmpty() && unregistrationMethods.isEmpty()))
			throw new IllegalArgumentException("target object needs to implement "
					+ OsgiServiceRegistrationListener.class.getName()
					+ " or custom registered/unregistered methods have to be specified");
	}

	/**
	 * Determine a custom method (if specified) on the given object. If the
	 * methodName is not null and no method is found, an exception is thrown. If
	 * the methodName is null/empty, an empty map is returned.
	 * 
	 * @param methodName
	 * @return
	 */
	protected Map determineCustomMethods(final String methodName) {
		if (!StringUtils.hasText(methodName)) {
			return Collections.EMPTY_MAP;
		}

		final Map methods = new LinkedHashMap(3);
		final boolean trace = log.isTraceEnabled();

		// find all methods that fit a certain description
		// since we don't have overloaded methods, look first for Maps and, if
		// nothing is found, then Dictionaries

		ReflectionUtils.doWithMethods(target.getClass(), new ReflectionUtils.MethodCallback() {

			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				// do matching on method name first
				if (methodName.equals(method.getName())) {
					// take a look at the parameter types
					Class[] args = method.getParameterTypes();
					if (args != null && args.length == 1) {
						Class propType = args[0];
						if (Dictionary.class.isAssignableFrom(propType) || Map.class.isAssignableFrom(propType)) {
							if (trace)
								log.trace("discovered custom method [" + method.toString() + "] on "
										+ target.getClass());
						}
						// see if there was a method already found
						Method m = (Method) methods.get(methodName);

						if (m != null) {
							if (trace)
								log.trace("there is already a custom method [" + m.toString() + "];ignoring " + method);
						}
						else {
							ReflectionUtils.makeAccessible(method);
							methods.put(methodName, method);
						}
					}
				}
			}
		});

		if (!methods.isEmpty())
			return methods;

		throw new IllegalArgumentException("incorrect custom method [" + methodName + "] specified on class "
				+ target.getClass());
	}

	public void registered(Map serviceProperties) {
		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("invoking registered method with props=" + serviceProperties);

		// first call interface method (if it exists)
		if (isListener) {
			if (trace)
				log.trace("invoking listener interface methods");

			try {
				((OsgiServiceRegistrationListener) target).registered(serviceProperties);
			}
			catch (Exception ex) {
				log.warn("standard registered method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}

		invokeCustomMethods(target, registrationMethods, serviceProperties);
	}

	public void unregistered(Map serviceProperties) {

		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("invoking unregistered method with props=" + serviceProperties);

		// first call interface method (if it exists)
		if (isListener) {
			if (trace)
				log.trace("invoking listener interface methods");

			try {
				((OsgiServiceRegistrationListener) target).unregistered(serviceProperties);
			}
			catch (Exception ex) {
				log.warn("standard unregistered method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}
		invokeCustomMethods(target, unregistrationMethods, serviceProperties);
	}

	/**
	 * Call the appropriate method (which have the key a type compatible of the
	 * given service) from the given method map.
	 * 
	 * @param target
	 * @param methods
	 * @param service
	 * @param properties
	 */
	// the properties field is Dictionary implementing a Map interface
	protected void invokeCustomMethods(Object target, Map methods, Map properties) {
		if (methods != null && !methods.isEmpty()) {
			boolean trace = log.isTraceEnabled();

			Object[] args = new Object[] { properties };
			for (Iterator iter = methods.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				// find the compatible types (accept null service)
				Method method = (Method) entry.getValue();
				if (trace)
					log.trace("invoking listener custom method " + method);

				try {
					ReflectionUtils.invokeMethod(method, target, args);
				}
				// make sure to log exceptions and continue with the
				// rest of
				// the listeners
				catch (Exception ex) {
					log.warn("custom method [" + method + "] threw exception when passing properties [" + properties
							+ "]", ex);
				}
			}
		}
	}

	/**
	 * @param registrationMethod The registrationMethod to set.
	 */
	public void setRegistrationMethod(String registrationMethod) {
		this.registrationMethod = registrationMethod;
	}

	/**
	 * @param unregistrationMethod The unregistrationMethod to set.
	 */
	public void setUnregistrationMethod(String unregistrationMethod) {
		this.unregistrationMethod = unregistrationMethod;
	}

}
