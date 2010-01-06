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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.AbstractBeanFactory;

/**
 * Default implementation for {@link ManagedServiceBeanManager}.
 * 
 * @author Costin Leau
 * 
 */
public class DefaultManagedServiceBeanManager implements DisposableBean, ManagedServiceBeanManager {

	/** logger */
	private static final Log log = LogFactory.getLog(DefaultManagedServiceBeanManager.class);

	private final Map<Integer, Object> instanceRegistry = new ConcurrentHashMap<Integer, Object>(8);
	private final UpdateCallback updateCallback;
	private final ConfigurationAdminManager cam;
	private final AbstractBeanFactory bf;

	public DefaultManagedServiceBeanManager(boolean autowireOnUpdate, String methodName,
			ConfigurationAdminManager cam, BeanFactory beanFactory) {

		updateCallback = CMUtils.createCallback(autowireOnUpdate, methodName, beanFactory);
		bf = (beanFactory instanceof AbstractBeanFactory ? (AbstractBeanFactory) beanFactory : null);
		this.cam = cam;
		this.cam.setBeanManager(this);
	}

	public Object register(Object bean) {
		int hashCode = System.identityHashCode(bean);
		if (log.isTraceEnabled())
			log.trace("Start tracking instance " + bean.getClass().getName() + "@" + hashCode);
		instanceRegistry.put(Integer.valueOf(hashCode), bean);
		applyInitialInjection(bean, cam.getConfiguration());
		return bean;
	}

	void applyInitialInjection(Object instance, Map configuration) {
		if (log.isTraceEnabled())
			log.trace("Applying injection to instance " + instance.getClass() + "@" + System.identityHashCode(instance)
					+ " using map " + configuration);
		CMUtils.applyMapOntoInstance(instance, configuration, bf);
	}

	public void unregister(Object bean) {
		int hashCode = System.identityHashCode(bean);
		if (log.isTraceEnabled())
			log.trace("Stopped tracking instance " + bean.getClass().getName() + "@" + hashCode);

		instanceRegistry.remove(new Integer(hashCode));
	}

	public void updated(Map properties) {
		if (updateCallback != null) {
			CMUtils.bulkUpdate(updateCallback, instanceRegistry.values(), properties);
		}
	}

	public void destroy() {
		// unregister CM services
		cam.destroy();
		// remove the tracked beans
		instanceRegistry.clear();
	}
}