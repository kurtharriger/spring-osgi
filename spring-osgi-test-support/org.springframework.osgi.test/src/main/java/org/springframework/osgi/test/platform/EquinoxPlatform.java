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

import java.lang.reflect.Field;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.BundleContext;

/**
 * Equinox (3.1.x and 3.2.x) OSGi platform.
 * 
 * @author Costin Leau
 * 
 */
public class EquinoxPlatform implements OsgiPlatform {

	/**
	 */
	private String[] ARGS = new String[] { "-clean" };

	private BundleContext context;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.OsgiPlatform#getBundleContext()
	 */
	public BundleContext getBundleContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.OsgiPlatform#start()
	 */
	public void start() throws Exception {

		// passed the configuration option as System properties then arguments
		// (there is no dependency on the main class).
		System.setProperty("eclipse.ignoreApp", "true");
		System.setProperty("osgi.clean", "true");
		System.setProperty("osgi.noShutdown", "true");
		// System.setProperty("osgi.console", "");

		// Equinox 3.1.x returns void - use of reflection is required
		// use main since in 3.1.x it sets up some system properties
		EclipseStarter.main(ARGS);

		Field field = EclipseStarter.class.getDeclaredField("context");
		field.setAccessible(true);
		context = (BundleContext) field.get(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.OsgiPlatform#stop()
	 */
	public void stop() throws Exception {
		EclipseStarter.shutdown();
	}

}
