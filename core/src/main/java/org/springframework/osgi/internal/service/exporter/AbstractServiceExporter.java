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
package org.springframework.osgi.internal.service.exporter;

import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.internal.service.ServiceExporter;

/**
 * @author Costin Leau
 * 
 */
public abstract class AbstractServiceExporter implements ServiceExporter, InitializingBean {

	protected boolean publishAtStartup = true;

	/** running monitor */
	private final Object monitor = new Object();

	private boolean running = false;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.internal.ServiceExporter#setPublishAtStartup(boolean)
	 */
	public void setPublishAtStartup(boolean publish) {
		this.publishAtStartup = publish;
	}

	public void afterPropertiesSet() throws Exception {
		if (publishAtStartup)
			start();
	}

	public boolean isRunning() {
		synchronized (monitor) {
			return running;			
		}
	}

	public void start() {
		synchronized (monitor) {
			if (!running) {
				running = true;
				registerService();
			}
		}
	}

	public void stop() {
		synchronized (monitor) {
			if (running) {
				running = false;
				unregisterService();
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
