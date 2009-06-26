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

package org.springframework.osgi.config.internal.adapter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.osgi.util.internal.ReflectionUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Local utility class used by adapters. Handles things such as method discovery.
 * 
 * 
 * @author Costin Leau
 * 
 */
public abstract class CustomListenerAdapterUtils {

	private static final Log log = LogFactory.getLog(CustomListenerAdapterUtils.class);

	/**
	 * Specialised reflection utility that determines all methods that accept two parameters such:
	 * 
	 * <pre> methodName(Type serviceType, Type1 arg)
	 * 
	 * methodName(Type serviceType, Type2 arg)
	 * 
	 * methodName(AnotherType serviceType, Type1 arg)
	 * 
	 * methodName(Type serviceType) </pre>
	 * 
	 * It will return a map which has the serviceType (first argument) as type and contains as list the variants of
	 * methods using the second argument. This method is normally used by listeners when determining custom methods.
	 * 
	 * @param methodName
	 * @param possibleArgumentTypes
	 * @return
	 */
	static Map<Class<?>, List<Method>> determineCustomMethods(final Class<?> target, final String methodName,
			final Class<?>[] possibleArgumentTypes) {

		if (!StringUtils.hasText(methodName)) {
			return Collections.emptyMap();
		}

		Assert.notEmpty(possibleArgumentTypes);

		final Map<Class<?>, List<Method>> methods = new LinkedHashMap<Class<?>, List<Method>>(3);

		final boolean trace = log.isTraceEnabled();

		org.springframework.util.ReflectionUtils.doWithMethods(target,
				new org.springframework.util.ReflectionUtils.MethodCallback() {

					public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
						if (!method.isBridge() && methodName.equals(method.getName())) {
							// take a look at the variables
							Class<?>[] args = method.getParameterTypes();

							if (args != null) {
								// Properties can be ignored
								if (args.length == 1) {
									addMethod(args[0], method, methods);
								}
								// or passed as Map, Dictionary
								else if (args.length == 2) {
									Class<?> propType = args[1];

									for (int i = 0; i < possibleArgumentTypes.length; i++) {
										Class<?> clazz = possibleArgumentTypes[i];
										if (clazz.isAssignableFrom(propType)) {
											addMethod(args[0], method, methods);
										}
									}
								}
							}
						}
					}

					private void addMethod(Class<?> key, Method mt, Map<Class<?>, List<Method>> methods) {

						if (trace)
							log.trace("discovered custom method [" + mt.toString() + "] on " + target);

						List<Method> mts = methods.get(key);
						if (mts == null) {
							mts = new ArrayList<Method>(2);
							methods.put(key, mts);
							org.springframework.util.ReflectionUtils.makeAccessible(mt);
							mts.add(mt);
							return;
						}
						// add a method only if there is still space
						if (mts.size() == 1) {
							Method m = mts.get(0);
							if (m.getParameterTypes().length == mt.getParameterTypes().length) {
								if (trace)
									log.trace("Method w/ signature " + methodSignature(m)
											+ " has been already discovered; ignoring method" + m.toString()
											+ "];ignoring " + m);
							} else {
								org.springframework.util.ReflectionUtils.makeAccessible(mt);
								mts.add(mt);
							}
						}
					}

					private String methodSignature(Method m) {
						StringBuilder sb = new StringBuilder();
						int mod = m.getModifiers();
						if (mod != 0) {
							sb.append(Modifier.toString(mod) + " ");
						}
						sb.append(m.getReturnType() + " ");
						sb.append(m.getName() + "(");
						Class<?>[] params = m.getParameterTypes();
						for (int j = 0; j < params.length; j++) {
							sb.append(params[j]);
							if (j < (params.length - 1))
								sb.append(",");
						}
						sb.append(")");
						return sb.toString();
					}
				});

		return methods;
	}

	/**
	 * Shortcut method that uses as possible argument types, Dictionary.class, Map.class or even nothing.
	 * 
	 * @param target
	 * @param methodName
	 * @return
	 */
	static Map<Class<?>, List<Method>> determineCustomMethods(Class<?> target, final String methodName) {
		return determineCustomMethods(target, methodName, new Class[] { Dictionary.class, Map.class });
	}

	/**
	 * Invoke the custom listener method. Takes care of iterating through the method map (normally acquired through
	 * {@link #determineCustomMethods(Class, String, Class[])} and invoking the method using the arguments.
	 * 
	 * @param target
	 * @param methods
	 * @param service
	 * @param properties
	 */
	// the properties field is Dictionary implementing a Map interface
	static void invokeCustomMethods(Object target, Map<Class<?>, List<Method>> methods, Object service,
			Map<?, ?> properties) {
		if (methods != null && !methods.isEmpty()) {
			boolean trace = log.isTraceEnabled();

			Object[] argsWMap = new Object[] { service, properties };
			Object[] argsWOMap = new Object[] { service };
			for (Iterator<Map.Entry<Class<?>, List<Method>>> iter = methods.entrySet().iterator(); iter.hasNext();) {
				Map.Entry<Class<?>, List<Method>> entry = iter.next();
				Class<?> key = entry.getKey();
				// find the compatible types (accept null service)
				if (service == null || key.isInstance(service)) {
					List<Method> mts = entry.getValue();
					for (Method method : mts) {
						if (trace)
							log.trace("Invoking listener custom method " + method);

						Class<?>[] argTypes = method.getParameterTypes();
						Object[] arguments = (argTypes.length > 1 ? argsWMap : argsWOMap);
						try {
							ReflectionUtils.invokeMethod(method, target, arguments);
						}
						// make sure to log exceptions and continue with the
						// rest of the methods
						catch (Exception ex) {
							Exception cause = ReflectionUtils.getInvocationException(ex);
							log.warn("Custom method [" + method + "] threw exception when passing service type ["
									+ (service != null ? service.getClass().getName() : null) + "]", cause);
						}
					}
				}
			}
		}
	}
}