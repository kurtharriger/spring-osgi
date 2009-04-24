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

package org.springframework.osgi.compendium.internal.cm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.util.StringUtils;

/**
 * Utility class that detects and then invokes custom destroy methods for
 * <managed-components>. Similar in functionality to DisposableBeanAdapter but
 * simpler since it is used just by templating beans. While it could be easily
 * implemented through a {@link DestructionAwareBeanPostProcessor}, due to the
 * order of the callbacks invocations and the overlap, an adapter becomes a
 * better choice.
 * 
 * @author Costin Leau
 * @see org.springframework.beans.factory.DisposableBean
 */
class ManagedFactoryDisposableInvoker {

	/** logger */
	private static final Log log = LogFactory.getLog(ManagedFactoryDisposableInvoker.class);

	private final boolean isDisposable;
	private final Method customSpringMethod;
	private final Object[] customSpringMethodArgs;
	private final Method customOsgiDestructionMethod;


	enum DestructionCodes {
		CM_ENTRY_DELETED(1), BUNDLE_STOPPING(2);

		private Integer value;


		/**
		 * Constructs a new <code>DestructionCodes</code> instance.
		 * 
		 * @param value
		 */
		DestructionCodes(int value) {
			this.value = Integer.valueOf(value);
		}

		public Integer getValue() {
			return value;
		}
	}


	/**
	 * Constructs a new <code>ManagedFactoryDisposableAdapter</code> instance.
	 * 
	 * @param methodName destruction method name
	 */
	public ManagedFactoryDisposableInvoker(Class<?> beanClass, String methodName) {
		this.isDisposable = DisposableBean.class.isAssignableFrom(beanClass);
		if (StringUtils.hasText(methodName)) {
			this.customSpringMethod = detectCustomSpringMethod(beanClass, methodName);

			if (customSpringMethod != null) {
				Class<?>[] types = customSpringMethod.getParameterTypes();
				this.customSpringMethodArgs = ((types.length == 1 && types[0].equals(boolean.class)) ? new Object[] { Boolean.TRUE }
						: null);
			}
			else {
				this.customSpringMethodArgs = null;
			}
			this.customOsgiDestructionMethod = detectCustomOsgiMethod(beanClass, methodName);
		}
		else {
			this.customSpringMethod = null;
			this.customSpringMethodArgs = null;
			this.customOsgiDestructionMethod = null;
		}
	}

	private Method detectCustomSpringMethod(Class<?> beanClass, String methodName) {
		Method m = BeanUtils.findMethod(beanClass, methodName, null);
		if (m == null) {
			m = BeanUtils.findMethod(beanClass, methodName, new Class[] { boolean.class });
		}
		return m;
	}

	private Method detectCustomOsgiMethod(Class<?> beanClass, String methodName) {
		return BeanUtils.findMethod(beanClass, methodName, new Class[] { int.class });
	}

	public void destroy(String beanName, Object beanInstance, DestructionCodes code) {
		// first invoke disposable bean
		if (isDisposable) {
			if (log.isDebugEnabled()) {
				log.debug("Invoking destroy() on bean with name '" + beanName + "'");
			}

			try {
				((DisposableBean) beanInstance).destroy();
			}
			catch (Throwable ex) {
				String msg = "Invocation of destroy method failed on bean with name '" + beanName + "'";
				if (log.isDebugEnabled()) {
					log.warn(msg, ex);
				}
				else {
					log.warn(msg + ": " + ex);
				}
			}
		}

		// custom callback (no argument)
		invokeCustomMethod(beanName, beanInstance);
		// custom callback (int argument)
		invokeCustomMethod(beanName, beanInstance, code);
	}

	private void invokeCustomMethod(String targetName, Object target) {
		if (customSpringMethod != null) {
			invokeMethod(customSpringMethod, customSpringMethodArgs, targetName, target);
		}
	}

	private void invokeCustomMethod(String targetName, Object target, DestructionCodes code) {
		if (customOsgiDestructionMethod != null) {
			invokeMethod(customOsgiDestructionMethod, new Object[] { code.getValue() }, targetName, target);
		}
	}

	private void invokeMethod(Method method, Object[] args, String targetName, Object target) {
		try {
			method.invoke(target, args);
		}
		catch (InvocationTargetException ex) {
			String msg = "Invocation of destroy method '" + method.getName() + "' failed on bean with name '"
					+ targetName + "'";
			if (log.isDebugEnabled()) {
				log.warn(msg, ex.getTargetException());
			}
			else {
				log.warn(msg + ": " + ex.getTargetException());
			}
		}
		catch (Throwable ex) {
			log.error("Couldn't invoke destroy method '" + method.getName() + "' on bean with name '" + targetName
					+ "'", ex);
		}
	}
}