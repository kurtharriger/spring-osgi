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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;

/**
 * OSGi specific class used for supporting 'lazy-activated' services. That is,
 * services which are not yet resolved (or loaded) but that are published in
 * advanced.
 * 
 * The wrapper triggers class loading (and thus lazy activation) on first use
 * and blocks until the application context is fully created.
 * 
 * @author Costin Leau
 */
public class LazyServiceFactory implements ServiceFactory {

	/** logger */
	private static final Log log = LogFactory.getLog(OsgiServiceFactoryBean.class);

	// FIXME: make this configurable
	private static final long lazyInitWait = 1000 * 60 * 5;

	// root of all things Java
	private static final String OBJECT = Object.class.getName();
	private final ObjectFactory<ServiceFactory> factory;
	private final Bundle bundle;
	private final ConfigurableOsgiBundleApplicationContext context;
	private volatile boolean initialized = false;


	public <T> LazyServiceFactory(ObjectFactory<ServiceFactory> factory, Bundle bundle,
			ConfigurableOsgiBundleApplicationContext context) {
		this.factory = factory;
		this.bundle = bundle;
		this.context = context;
	}

	public Object getService(Bundle bundle, ServiceRegistration registration) {
		if (!initialized) {
			initialized = true;
			triggerLazyActivation(bundle);
			waitForContextRefresh();
		}
		return factory.getObject().getService(bundle, registration);
	}

	private void triggerLazyActivation(Bundle bnd) {
		try {
			bnd.loadClass(OBJECT);
		}
		catch (Exception ex) {
			// ignore it
		}
	}

	private void waitForContextRefresh() {
		try {
			CountDownLatch latch = LazyLatchFactory.getLatch(System.identityHashCode(context));
			if (latch != null && !latch.await(lazyInitWait, TimeUnit.MILLISECONDS)) {
				throw new IllegalStateException("Lazy exported service timed out waiting for startup of context "
						+ context.getDisplayName());
			}
		}
		catch (InterruptedException ex) {
			String message = "Lazy exported service from context " + context.getDisplayName() + " was interrupted";
			log.info(message);
			throw new IllegalStateException(message, ex);
		}
	}

	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		factory.getObject().ungetService(bundle, registration, service);
	}
}