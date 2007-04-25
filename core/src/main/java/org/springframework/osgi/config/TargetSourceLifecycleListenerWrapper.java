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
package org.springframework.osgi.config;

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

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
	 * type expected
	 */
	private Map bindMethods, unbindMethods;

	private String bindMethod, unbindMethod;

	private Object target;

	private boolean isLifecycleListener;

	public TargetSourceLifecycleListenerWrapper(Object object) {
		this.target = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(target, "target property required");

		isLifecycleListener = target instanceof TargetSourceLifecycleListener;

		if (isLifecycleListener)
			if (log.isDebugEnabled())
				log.debug(target.getClass().getName() + " is a lifecycle listener");
		bindMethods = determineCustomMethods(bindMethod);
		unbindMethods = determineCustomMethods(unbindMethod);

		if (!isLifecycleListener && (bindMethods == null || unbindMethods == null))
			throw new IllegalArgumentException("target object needs to implement "
					+ TargetSourceLifecycleListener.class + " or custom bind/unbind methods have to be specified");
	}

	/**
	 * Determine a custom method (if specified) on the given object. If the
	 * methodName is not null and no method is found, an exception is thrown.
	 * 
	 * @param methodName
	 * @return
	 */
	private Map determineCustomMethods(final String methodName) {
		if (methodName == null) {
			return null;
		}

		final Map methods = new HashMap(3);

		ReflectionUtils.doWithMethods(target.getClass(), new ReflectionUtils.MethodCallback() {

			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				if (methodName.equals(method.getName())) {
					// take a look at the variables
					Class[] args = method.getParameterTypes();
					if (args != null && args.length == 2) {
						if (Dictionary.class.isAssignableFrom(args[1])) {
							if (log.isDebugEnabled())
								log.debug("discovered custom method [" + method.toString() + "] on "
										+ target.getClass());
							methods.put(args[0], method);
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
	 * Call the method approapriate (which have the key an instance of the given
	 * service) from the given method map.
	 * 
	 * @param target
	 * @param methods
	 * @param service
	 * @param properties
	 */
	protected void invokeCustomMethods(Object target, Map methods, Object service, Dictionary properties) {
		if (methods != null && !methods.isEmpty()) {
			Object[] args = new Object[] { service, properties };
			for (Iterator iter = methods.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				Class key = (Class) entry.getKey();
				// find the compatible types (accept null service)
				if (service == null || key.isInstance(service)) {
					try {
						ReflectionUtils.invokeMethod((Method) entry.getValue(), target, args);
					}
					// make sure to log exceptions and continue with the rest of
					// the listeners
					catch (Exception ex) {
						log.warn("custom method [" + entry.getValue() + "] threw exception when passing service ["
								+ (service != null ? service.getClass().getName() : null) + "]", ex);
					}
				}
			}
		}
	}

	public void bind(Object service, Dictionary properties) throws Exception {
		// first call interface method (if it exists)
		if (isLifecycleListener) {
			try {
				((TargetSourceLifecycleListener) target).bind(service, properties);
			}
			catch (Exception ex) {
				log.warn("standard bind method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}

		invokeCustomMethods(target, bindMethods, service, properties);
	}

	public void unbind(Object service, Dictionary properties) throws Exception {
		// first call interface method (if it exists)
		if (isLifecycleListener) {
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
