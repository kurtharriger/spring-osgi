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

package org.springframework.osgi.extender.internal.activator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.WeakHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.GenericTypeResolver;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;

/**
 * Listener interface that delegates to a list of listener. This is useful in OSGi environments when dealing with
 * dynamic collections which can be updated during iteration.
 * 
 * @author Costin Leau
 * 
 */
@SuppressWarnings("unchecked")
class ListListenerAdapter implements OsgiBundleApplicationContextListener<OsgiBundleApplicationContextEvent>,
		InitializingBean, DisposableBean {

	private final ServiceTracker tracker;
	private final Map<Class<? extends OsgiBundleApplicationContextListener>, Class<? extends OsgiBundleApplicationContextEvent>> eventCache =
			new WeakHashMap<Class<? extends OsgiBundleApplicationContextListener>, Class<? extends OsgiBundleApplicationContextEvent>>();

	/**
	 * Constructs a new <code>ListListenerAdapter</code> instance.
	 * 
	 * @param listeners
	 */
	public ListListenerAdapter(BundleContext bundleContext) {
		this.tracker = new ServiceTracker(bundleContext, OsgiBundleApplicationContextListener.class.getName(), null);
	}

	public void afterPropertiesSet() {
		this.tracker.open();
	}

	public void destroy() {
		this.tracker.close();
		eventCache.clear();
	}

	public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
		Object[] listeners = tracker.getServices();

		if (listeners != null) {
			synchronized (eventCache) {
				for (Object listnr : listeners) {
					OsgiBundleApplicationContextListener listener = (OsgiBundleApplicationContextListener) listnr;
					Class<? extends OsgiBundleApplicationContextListener> listenerClass = listener.getClass();
					Class<? extends OsgiBundleApplicationContextEvent> eventType = eventCache.get(listenerClass);
					if (eventType == null) {
						Class<?> evtType =
								GenericTypeResolver.resolveTypeArgument(listenerClass,
										OsgiBundleApplicationContextListener.class);
						if (evtType != null && evtType.isAssignableFrom(OsgiBundleApplicationContextEvent.class)) {
							eventCache.put(listenerClass, eventType);
						} else {
							eventCache.put(listenerClass, OsgiBundleApplicationContextEvent.class);
						}
					}
					if (eventType.isInstance(event)) {
						listener.onOsgiApplicationEvent(event);
					}
				}
			}
		}
	}
}