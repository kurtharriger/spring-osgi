/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.osgi.test.platform;

import java.util.Properties;

import org.osgi.framework.BundleContext;

/**
 * Lifecycle contract for the OSGi platform.
 * 
 * @author Costin Leau
 * 
 */
public interface OsgiPlatform {

	/**
	 * Start the OSGi platform.
	 * 
	 * @throws Exception
	 */
	void start() throws Exception;

	/**
	 * Stop the OSGi platform.
	 * 
	 * @throws Exception
	 */
	void stop() throws Exception;

	/**
	 * Return the {@link java.util.Properties} object used for configuring the
	 * underlying OSGi implementation before starting it.
	 * 
	 * @return
	 */
	Properties getConfigurationProperties();

	/**
	 * Get a hold of the bundle context of the returned platform.
	 * @return
	 */
	BundleContext getBundleContext();
}
