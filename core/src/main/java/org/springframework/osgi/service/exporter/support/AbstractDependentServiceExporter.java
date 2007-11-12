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
package org.springframework.osgi.service.exporter.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.service.dependency.DependentServiceExporter;


/**
 * Base class for ServiceExporters taking care of service registration and
 * unregistration.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractDependentServiceExporter implements DependentServiceExporter, InitializingBean {

	private static final Log log = LogFactory.getLog(AbstractDependentServiceExporter.class);

	protected boolean publishAtStartup = true;

	/** running monitor */
	private final Object monitor = new Object();

	private boolean running = false;

	private boolean initialized = false;

	public void setPublishAtStartup(boolean publish) {
		this.publishAtStartup = publish;
	}

	public void afterPropertiesSet() throws Exception {
		synchronized (monitor) {
			initialized = true;
			if (publishAtStartup)
				start();
		}
	}

	public boolean isRunning() {
		synchronized (monitor) {
			return running;
		}
	}

	public void start() {
		synchronized (monitor) {
			if (!running) {
				if (initialized) {
					running = true;
					registerService();
				}
				else
					publishAtStartup = true;
			}
		}
		if (!initialized)
			log.trace("exporter not initialized, service not exported but registered for publication at startup");
	}

	public void stop() {
		synchronized (monitor) {
			if (running) {
				if (initialized) {
					running = false;
					unregisterService();
				}
				else
					publishAtStartup = false;
			}
		}
	}

	/**
	 * Register/Export the OSGi service.
	 */
	protected abstract void registerService();

	/**
	 * Unregister/de-export the OSGi service.
	 */
	protected abstract void unregisterService();

}
