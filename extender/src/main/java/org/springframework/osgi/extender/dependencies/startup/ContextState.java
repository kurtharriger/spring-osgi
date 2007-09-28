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
package org.springframework.osgi.extender.dependencies.startup;

import org.springframework.core.enums.StaticLabeledEnum;

/**
 * State of an application context while being processed by
 * {@link DependencyWaiterApplicationContextExecutor}.
 * 
 * This enumeration holds the state of an application context at a certain time,
 * beyond the official states such as STARTED/STOPPED.
 * 
 * @author Hal Hildebrand
 * @author Costin Leau
 * 
 */
public class ContextState extends StaticLabeledEnum {

	/**
	 * Application context has been initialized but not started (i.e. refresh
	 * hasn't been called).
	 */
	public static final ContextState INITIALIZED = new ContextState(0, "INITIALIZED");

	/**
	 * Application context has been started but the OSGi service dependencies
	 * haven't been yet resolved.
	 */
	public static final ContextState RESOLVING_DEPENDENCIES = new ContextState(1, "RESOLVING_DEPENDENCIES");

	/**
	 * Application context has been started and the OSGi dependencies have been
	 * resolved. However the context is not fully initialized (i.e. refresh
	 * hasn't been completed).
	 */
	public static final ContextState DEPENDENCIES_RESOLVED = new ContextState(2, "DEPENDENCIES_RESOLVED");

	/**
	 * Application context has been fully initialized. The OSGi dependencies
	 * have been resolved and refresh has fully completed.
	 */
	public static final ContextState STARTED = new ContextState(3, "STARTED");

	/**
	 * Application context has been interruped. This state occurs if the context
	 * is being closed before being fully started.
	 */
	public static final ContextState INTERRUPTED = new ContextState(4, "INTERRUPTED");

	/**
	 * Application context has been stopped. This can occur even only if the
	 * context has been fully started for example; otherwise
	 * {@link #INTERRUPTED} state should be used.
	 */
	public static final ContextState STOPPED = new ContextState(5, "STOPPED");

	private ContextState(int value, String name) {
		super(value, name);
	}

	/**
	 * The state indicates a 'down' context - that is a context which has been
	 * either closed or stopped.
	 * 
	 * @return true if the context has been interrupted or stopped, false
	 * otherwise.
	 */
	public boolean isDown() {
		return (this == INTERRUPTED || this == STOPPED);
	}
}
