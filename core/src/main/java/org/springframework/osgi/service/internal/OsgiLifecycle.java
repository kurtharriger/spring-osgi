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
package org.springframework.osgi.service.internal;

/**
 * Lifecycle interface for OSGi components. Provides control over the OSGi
 * behavior of Spring beans. A good example is an OSGi service exporter which,
 * during its lifetime, can register and unregister a bean several times based
 * on various factors.
 * 
 * Thus, the OSGi lifecycle is separate from that of the bean inside the Spring
 * container.
 * 
 * @author Costin Leau
 * 
 */
public interface OsgiLifecycle {

	/**
	 * Start the OSGi lifecycle.
	 * 
	 * Should not throw an exception if the component is already running.
	 */
	void start();

	/**
	 * Stop the OSGi lifecycle.
	 * 
	 * Should not throw an exception if the component is already stopped.
	 */
	void stop();

	/**
	 * Check whether this OSGi component is currently running.
	 * 
	 * @return
	 */
	boolean isRunning();
}
