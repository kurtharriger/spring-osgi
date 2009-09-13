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
package org.springframework.osgi.test.platform;

import org.apache.commons.logging.Log;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.springframework.osgi.util.OsgiPlatformDetector;
import org.springframework.util.Assert;

/**
 * Utility class useful for starting and stopping an OSGi 4.2 framework.
 * 
 * @author Costin Leau
 */
class DefaultFrameworkTemplate implements FrameworkTemplate {

	private final Framework fwk;
	/** logger */
	private final Log log;

	public DefaultFrameworkTemplate(Object target, Log log) {
		if (OsgiPlatformDetector.isR42()) {
			Assert.isInstanceOf(Framework.class, target);
			fwk = (Framework) target;
		} else {
			throw new IllegalStateException("Cannot use OSGi 4.2 Framework API in an OSGi 4.1 environment");
		}
		this.log = log;
	}

	public void init() {
		try {
			fwk.init();
		} catch (BundleException ex) {
			throw new IllegalStateException("Cannot initialize framework", ex);
		}
	}

	public void start() {
		try {
			fwk.start();
		} catch (BundleException ex) {
			throw new IllegalStateException("Cannot start framework", ex);
		}
	}

	public void stopAndWait(long delay) {
		try {
			fwk.stop();
		} catch (BundleException ex) {
			log.error("Cannot stop framework", ex);
		}

		try {
			fwk.waitForStop(delay);
		} catch (InterruptedException ex) {
			log.error("Waiting for framework to stop interrupted", ex);
		}
	}
}