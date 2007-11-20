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

import java.io.File;
import java.util.Properties;

import org.knopflerfish.framework.Framework;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.test.internal.util.IOUtils;

/**
 * Knopflerfish 2.x Platform.
 * 
 * @author Costin Leau
 * 
 */
public class KnopflerfishPlatform extends AbstractOsgiPlatform {

	private BundleContext context;

	private Framework framework;

	private File kfStorageDir;

	public KnopflerfishPlatform() {
		toString = "Knopflerfish OSGi Platform";
	}

	protected Properties getPlatformProperties() {
		kfStorageDir = createTempDir("kf");

		// default properties
		Properties props = new Properties();
		props.setProperty("org.osgi.framework.dir", kfStorageDir.getAbsolutePath());
		props.setProperty("org.knopflerfish.framework.bundlestorage", "file");
		props.setProperty("org.knopflerfish.framework.bundlestorage.file.reference", "true");
		props.setProperty("org.knopflerfish.framework.bundlestorage.file.unpack", "false");
		props.setProperty("org.knopflerfish.startlevel.use", "true");
		props.setProperty("org.knopflerfish.osgi.setcontextclassloader", "true");
		// TODO: set this to false for the moment since it causes NPEs
		// let service be available during unregistration
		props.setProperty("org.knopflerfish.servicereference.valid.during.unregistering", "false");
		// embedded mode
		props.setProperty("org.knopflerfish.framework.exitonshutdown", "false");
		// disable patch CL
		props.setProperty("org.knopflerfish.framework.patch", "false");
		return props;
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
		try {
			framework.shutdown();
		}
		finally {
			IOUtils.delete(kfStorageDir);
		}
	}
}
