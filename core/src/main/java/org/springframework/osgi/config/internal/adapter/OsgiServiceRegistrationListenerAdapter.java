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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.service.exporter.OsgiServiceRegistrationListener;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapter/wrapper class that handles listener with custom method invocation. Similar in functionality to
 * {@link org.springframework.osgi.config.internal.adapter.OsgiServiceLifecycleListenerAdapter}.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceRegistrationListenerAdapter implements OsgiServiceRegistrationListener, InitializingBean,
		BeanFactoryAware {

	private static final Log log = LogFactory.getLog(OsgiServiceRegistrationListenerAdapter.class);

	/** does the target implement the listener interface */
	private boolean isListener;

	private String registrationMethod, unregistrationMethod;

	/** actual target */
	private Object target;

	/** target bean name (when dealing with cycles) */
	private String targetBeanName;

	/** bean factory used for retrieving the target when dealing with cycles */
	private BeanFactory beanFactory;

	/** init flag */
	private boolean initialized;

	/**
	 * Map of methods keyed by the first parameter which indicates the service type expected.
	 */
	private Map<Class<?>, List<Method>> registrationMethods, unregistrationMethods;

	public void afterPropertiesSet() {
		Assert.notNull(beanFactory);
		Assert.isTrue(target != null || StringUtils.hasText(targetBeanName),
				"one of 'target' or 'targetBeanName' properties has to be set");

		if (target != null)
			initialized = true;

		// do validation (on the target type)
		initialize();
		// postpone target initialization until one of bind/unbind method is called
	}

	private void retrieveTarget() {
		target = beanFactory.getBean(targetBeanName);
		initialized = true;
	}

	/**
	 * Initialise adapter. Determine custom methods and do validation.
	 */
	private void initialize() {
		Class<?> clazz = (target == null ? beanFactory.getType(targetBeanName) : target.getClass());

		isListener = OsgiServiceRegistrationListener.class.isAssignableFrom(clazz);
		if (isListener)
			if (log.isDebugEnabled())
				log.debug(clazz.getName() + " is a registration listener");

		registrationMethods = CustomListenerAdapterUtils.determineCustomMethods(clazz, registrationMethod);
		unregistrationMethods = CustomListenerAdapterUtils.determineCustomMethods(clazz, unregistrationMethod);

		if (!isListener && (registrationMethods.isEmpty() && unregistrationMethods.isEmpty()))
			throw new IllegalArgumentException("target object needs to implement "
					+ OsgiServiceRegistrationListener.class.getName()
					+ " or custom registered/unregistered methods have to be specified");
	}

	public void registered(Object service, Map<?, ?> serviceProperties) {
		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("invoking registered method with props=" + serviceProperties);

		if (!initialized)
			retrieveTarget();

		// first call interface method (if it exists)
		if (isListener) {
			if (trace)
				log.trace("invoking listener interface methods");

			try {
				((OsgiServiceRegistrationListener) target).registered(service, serviceProperties);
			} catch (Exception ex) {
				log.warn("standard registered method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}

		CustomListenerAdapterUtils.invokeCustomMethods(target, registrationMethods, service, serviceProperties);
	}

	public void unregistered(Object service, Map<?, ?> serviceProperties) {

		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("invoking unregistered method with props=" + serviceProperties);

		if (!initialized)
			retrieveTarget();

		// first call interface method (if it exists)
		if (isListener) {
			if (trace)
				log.trace("invoking listener interface methods");

			try {
				((OsgiServiceRegistrationListener) target).unregistered(service, serviceProperties);
			} catch (Exception ex) {
				log.warn("standard unregistered method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}
		CustomListenerAdapterUtils.invokeCustomMethods(target, unregistrationMethods, service, serviceProperties);
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

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * @param target The target to set.
	 */
	public void setTarget(Object target) {
		this.target = target;
	}

	/**
	 * @param targetBeanName The targetBeanName to set.
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}
}