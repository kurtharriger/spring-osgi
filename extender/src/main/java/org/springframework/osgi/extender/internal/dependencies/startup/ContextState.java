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
package org.springframework.osgi.extender.internal.dependencies.startup;

/**
 * State of an application context while being processed by {@link DependencyWaiterApplicationContextExecutor}.
 * 
 * This enumeration holds the state of an application context at a certain time, beyond the official states such as
 * STARTED/STOPPED.
 * 
 * @author Hal Hildebrand
 * @author Costin Leau
 * 
 */
public enum ContextState {

	/**
	 * Application context has been initialized but not started (i.e. refresh hasn't been called).
	 */
	INITIALIZED,

	/**
	 * Application context has been started but the OSGi service dependencies haven't been yet resolved.
	 */
	RESOLVING_DEPENDENCIES,

	/**
	 * Application context has been started and the OSGi dependencies have been resolved. However the context is not
	 * fully initialized (i.e. refresh hasn't been completed).
	 */
	DEPENDENCIES_RESOLVED,

	/**
	 * Application context has been fully initialized. The OSGi dependencies have been resolved and refresh has fully
	 * completed.
	 */
	STARTED,

	/**
	 * Application context has been interrupted. This state occurs if the context is being closed before being fully
	 * started.
	 */
	INTERRUPTED,

	/**
	 * Application context has been stopped. This can occur even only if the context has been fully started for example;
	 * otherwise {@link #INTERRUPTED} state should be used.
	 */
	STOPPED;

	/**
	 * Indicates whether the state is 'down' or not - that is a context which has been either closed or stopped.
	 * 
	 * @return true if the context has been interrupted or stopped, false otherwise.
	 */
	public boolean isDown() {
		return (this.equals(INTERRUPTED) || this.equals(STOPPED));
	}

	/**
	 * Indicates whether the state is unresolved or not. An unresolved state means a state which is active (started) in
	 * RESOLVING_DEPENDENCIES state.
	 * 
	 * @return
	 */
	public boolean isUnresolved() {
		return (this.equals(RESOLVING_DEPENDENCIES) || this.equals(INITIALIZED));
	}

	public boolean isResolved() {
		return !isUnresolved();
	}
}