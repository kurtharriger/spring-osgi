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
package org.springframework.osgi.test.platform;

import java.io.File;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.osgi.test.util.IOUtils;
import org.springframework.util.ObjectUtils;

import com.prosyst.mbs.impl.framework.BundleContextImpl;
import com.prosyst.mbs.impl.framework.Framework;
import com.prosyst.mbs.impl.framework.Start;

/**
 * Prosyst mBedded Server Professional Edition 6.1.x
 * 
 * @author Costin Leau
 */
public class MBSProPlatform extends AbstractOsgiPlatform {

	private BundleContext context;

	private File storageDir;

	public MBSProPlatform() {
		toString = "mBedded Professional OSGi Platform";
	}

	protected Properties getPlatformProperties() {
		Properties props = new Properties();

		// default properties
		props.setProperty("mbs.loader.r4", "true");
		props.setProperty("mbs.storage.delete", "true");

		// this affects only the connector version but still
		props.setProperty("mbs.storage.mm.type", "ram");
		props.setProperty("mbs.certificates", "false");

		// props.setProperty("mbs.boot.disableProcessing", "true");

		props.setProperty(Constants.SUPPORTS_FRAMEWORK_FRAGMENT, "true");
		props.setProperty(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE, "true");

		props.setProperty("mbs.debug", "0");

		props.putAll(getTemporaryStorage());

		return props;
	}

	protected Properties getTemporaryStorage() {
		Properties props = new Properties();
		storageDir = createTempDir("mbs");

		props.setProperty("mbs.storage.root", storageDir.getAbsolutePath());

		if (log.isTraceEnabled())
			log.trace("mBedded temporary storage dir is " + storageDir.getAbsolutePath());
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
		Properties props = getConfigurationProperties();
		System.getProperties().putAll(props);
		Start.main(new String[0]);

		context = BundleContextImpl.nullCtx;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.OsgiPlatform#stop()
	 */
	public void stop() throws Exception {
		try {
			Framework.systemBundle.stop();
		}
		finally {
			IOUtils.delete(storageDir);
		}
	}
}
