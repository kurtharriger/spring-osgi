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
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.internal.util.ReflectionUtils;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.util.StringUtils;

/**
 * TargetSourceLifecycleListener wrapper for custom beans, useful when custom
 * methods are being used.
 * 
 * @author Costin Leau
 * 
 */
public class TargetSourceLifecycleListenerWrapper implements TargetSourceLifecycleListener, InitializingBean {

	private static final Log log = LogFactory.getLog(TargetSourceLifecycleListenerWrapper.class);

	/**
	 * Map of methods keyed by the first parameter which indicates the service
	 * type expected.
	 */
	private Map bindMethods, unbindMethods;

	private String bindMethod, unbindMethod;

	private final Object target;

	private final boolean isLifecycleListener;

	public TargetSourceLifecycleListenerWrapper(Object object) {
		this.target = object;
		isLifecycleListener = target instanceof TargetSourceLifecycleListener;
	}

	/**
	 * Initialise adapter. Determine custom methods and do validation.
	 */
	public void afterPropertiesSet() {

		if (isLifecycleListener)
			if (log.isDebugEnabled())
				log.debug(target.getClass().getName() + " is a lifecycle listener");
		bindMethods = determineCustomMethods(bindMethod);
		unbindMethods = determineCustomMethods(unbindMethod);

		if (!isLifecycleListener && (bindMethods.isEmpty() && unbindMethods.isEmpty()))
			throw new IllegalArgumentException("target object needs to implement "
					+ TargetSourceLifecycleListener.class.getName()
					+ " or custom bind/unbind methods have to be specified");
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

		org.springframework.util.ReflectionUtils.doWithMethods(target.getClass(),
			new org.springframework.util.ReflectionUtils.MethodCallback() {

				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					if (methodName.equals(method.getName())) {
						// take a look at the variables
						Class[] args = method.getParameterTypes();

						// Properties can be passed as Map or Dictionary
						if (args != null && args.length == 2) {
							Class propType = args[1];
							if (Dictionary.class.isAssignableFrom(propType) || Map.class.isAssignableFrom(propType)) {
								if (trace)
									log.trace("discovered custom method [" + method.toString() + "] on "
											+ target.getClass());

								// see if there was a method already found
								Method m = (Method) methods.get(args[0]);

								if (m != null) {
									if (trace)
										log.trace("type " + args[0] + " already has an associated method ["
												+ m.toString() + "];ignoring " + method);
								}
								else {
									org.springframework.util.ReflectionUtils.makeAccessible(method);
									methods.put(args[0], method);
								}
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
	protected void invokeCustomMethods(Object target, Map methods, Object service, Map properties) {
		if (methods != null && !methods.isEmpty()) {
			boolean trace = log.isTraceEnabled();

			Object[] args = new Object[] { service, properties };
			for (Iterator iter = methods.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				Class key = (Class) entry.getKey();
				Method method = (Method) entry.getValue();
				// find the compatible types (accept null service)
				if (service == null || key.isInstance(service)) {
					if (trace)
						log.trace("invoking listener custom method " + method);

					try {
						ReflectionUtils.invokeMethod(method, target, args);
					}
					// make sure to log exceptions and continue with the
					// rest of
					// the listeners
					catch (Exception ex) {
						Exception cause = ReflectionUtils.getInvocationException(ex);
						log.warn("custom method [" + method + "] threw exception when passing service type ["
								+ (service != null ? service.getClass().getName() : null) + "]", cause);
					}
				}
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
				((TargetSourceLifecycleListener) target).bind(service, properties);
			}
			catch (Exception ex) {
				log.warn("standard bind method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}

		invokeCustomMethods(target, bindMethods, service, properties);
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
				((TargetSourceLifecycleListener) target).unbind(service, properties);
			}
			catch (Exception ex) {
				log.warn("standard unbind method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}

		invokeCustomMethods(target, unbindMethods, service, properties);
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
