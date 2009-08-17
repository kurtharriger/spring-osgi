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
package org.springframework.osgi.service.exporter.support.internal.support;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;

/**
 * Class encapsulating the lazy dependency lookup of the target bean. Handles caching as well as the lazy listener
 * notifications.
 * 
 * @author Costin Leau
 */
public class LazyTargetResolver implements UnregistrationNotifier {

	private final BeanFactory beanFactory;
	private final String beanName;
	private final boolean cacheService;
	private volatile Object target;
	private final Object lock = new Object();
	private final AtomicBoolean activated;
	private final ListenerNotifier notifier;
	private volatile ServiceRegistrationDecorator decorator;

	public LazyTargetResolver(Object target, BeanFactory beanFactory, String beanName, boolean cacheService,
			ListenerNotifier notifier, boolean lazyListeners) {
		this.target = target;
		this.beanFactory = beanFactory;
		this.beanName = beanName;
		this.cacheService = cacheService;
		this.notifier = notifier;
		this.activated = new AtomicBoolean(!lazyListeners);
	}

	public void activate() {
		if (activated.compareAndSet(false, true)) {
			// no service registered
			if (decorator == null) {
				notifier.callUnregister(null, null);
			} else {
				Object target = getBeanIfPossible();
				Map properties = (Map) OsgiServiceReferenceUtils.getServicePropertiesSnapshot(decorator.getReference());
				notifier.callRegister(target, properties);
			}
		}
	}

	private Object getBeanIfPossible() {
		if (target == null) {
			if (cacheService || beanFactory.isSingleton(beanName)) {
				getBean();
			}
		}

		return target;
	}

	public Object getBean() {
		if (target == null) {
			if (cacheService) {
				synchronized (lock) {
					if (target == null) {
						target = beanFactory.getBean(beanName);
					}
				}
			} else {
				return beanFactory.getBean(beanName);
			}
		}
		return target;
	}

	public Class<?> getType() {
		return (target == null ? (beanFactory.isSingleton(beanName) ? beanFactory.getBean(beanName).getClass()
				: beanFactory.getType(beanName)) : target.getClass());

	}

	public void unregister(Map properties) {
		if (activated.get()) {
			Object target = getBeanIfPossible();
			notifier.callUnregister(target, properties);
		}
	}

	public void setDecorator(ServiceRegistrationDecorator decorator) {
		this.decorator = decorator;
		if (decorator != null) {
			decorator.setNotifier(this);
		}
	}

	public void notifyIfPossible() {
		if (activated.get() && notifier != null) {
			Object target = getBeanIfPossible();
			Map properties = (Map) OsgiServiceReferenceUtils.getServicePropertiesSnapshot(decorator.getReference());
			notifier.callRegister(target, properties);
		}
	}

	// called when the exporter is activated but no service is published
	public void startupUnregisterIfPossible() {
		if (activated.get() && notifier != null) {
			notifier.callUnregister(null, null);
		}
	}
}