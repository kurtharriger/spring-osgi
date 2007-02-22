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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.knopflerfish.framework.Framework;
import org.osgi.framework.BundleContext;

/**
 * Knopflerfish 2.x Platform.
 * 
 * @author Costin Leau
 * 
 */
public class KnopflerfishPlatform extends AbstractOsgiPlatform {

	private static final Log log = LogFactory.getLog(KnopflerfishPlatform.class);
	
	private BundleContext context;

	private Framework framework;

	public KnopflerfishPlatform() {
		toString = "Knopflerfish OSGi Platform";
		
		// default properties
		Properties props = getConfigurationProperties();
		props.setProperty("org.knopflerfish.framework.bundlestorage", "memory");
		props.setProperty("org.knopflerfish.startlevel.use", "true");
		props.setProperty("org.knopflerfish.osgi.setcontextclassloader", "true");

	}

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
		// copy configuration properties to sys properties
		System.getProperties().putAll(getConfigurationProperties());
		
		framework = new Framework(this);
		framework.launch(0);
		context = framework.getSystemBundleContext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.OsgiPlatform#stop()
	 */
	public void stop() throws Exception {
		framework.shutdown();
	}
}
