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
package org.springframework.osgi.internal.context;

import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;

/**
 * Interface that redirect the application context crucial methods to a third
 * party executor. This interface splits the refresh method in two:
 * {@link #preRefresh()} and {@link #postRefresh()}.
 * 
 * @author Costin Leau
 * 
 */
public interface DelegatedExecutionOsgiBundleApplicationContext extends ConfigurableOsgiBundleApplicationContext {

	/**
	 * The non-delegated refresh operation.
	 * 
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	void normalRefresh();

	/**
	 * The non-delegated close operation.
	 * 
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	void normalClose();

	/**
	 * First phase of the refresh. Executes right a certain condition, imposed
	 * by the executor is checked. Normally, this just prepares the beanFactory
	 * but does not initialize any beans.
	 */
	void preRefresh();

	/**
	 * The second, last phase of the refresh. Finishes the rest of the refresh
	 * operation. Normally, this operations performs most of the refresh work,
	 * such as instantiating singleton.
	 */
	void postRefresh();

	/**
	 * Assign the {@link OsgiBundleApplicationContextExecutor} for this delegated context.
	 * 
	 * @param executor
	 */
	void setExecutor(OsgiBundleApplicationContextExecutor executor);

	/**
	 * Synchronization monitor for this
	 * {@link org.springframework.context.ApplicationContext} in case multiple
	 * threads can work with the application context lifecycle.
	 * 
	 * @return monitor for this application context.
	 */
	Object getMonitor();

}
