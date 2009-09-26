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

package org.springframework.osgi.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.GenericTypeResolver;

/**
 * Listener dispatching OSGi events to interested listeners. This class acts mainly as an adapter bridging the
 * {@link ApplicationListener} interface with {@link OsgiBundleApplicationContextListener}.
 * 
 * @author Costin Leau
 * 
 */
class ApplicationListenerAdapter<E extends OsgiBundleApplicationContextEvent> implements SmartApplicationListener {

	private final OsgiBundleApplicationContextListener<E> osgiListener;
	private final Class<?> eventType;
	private final String toString;

	static <E extends OsgiBundleApplicationContextEvent> ApplicationListenerAdapter<E> createAdapter(
			OsgiBundleApplicationContextListener<E> listener) {
		return new ApplicationListenerAdapter<E>(listener);
	}

	private ApplicationListenerAdapter(OsgiBundleApplicationContextListener<E> listener) {
		this.osgiListener = listener;
		Class<?> evtType =
				GenericTypeResolver
						.resolveTypeArgument(listener.getClass(), OsgiBundleApplicationContextListener.class);
		this.eventType = (evtType == null ? OsgiBundleApplicationContextEvent.class : evtType);

		toString = "ApplicationListenerAdapter for listener " + osgiListener;
	}

	@SuppressWarnings("unchecked")
	public void onApplicationEvent(ApplicationEvent event) {
		if (eventType.isInstance(event)) {
			osgiListener.onOsgiApplicationEvent((E) event);
		}
	}

	public boolean equals(Object obj) {
		return osgiListener.equals(obj);
	}

	public int hashCode() {
		return osgiListener.hashCode();
	}

	public String toString() {
		return toString;
	}

	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return (eventType != null && eventType.isAssignableFrom(eventType));
	}

	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}
}