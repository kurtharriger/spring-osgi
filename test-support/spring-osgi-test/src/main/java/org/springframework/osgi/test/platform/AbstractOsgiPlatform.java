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
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for OsgiPlatform classes. Provides common functionality such as
 * creation a temporary folder on startup and removal on shutdown.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractOsgiPlatform implements OsgiPlatform {

	protected Log log = LogFactory.getLog(getClass());

	/**
	 * Subclasses should override this field.
	 */
	protected String toString = getClass().getName();

	protected Properties configurationProperties = new Properties();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.test.platform.OsgiPlatform#getConfigurationProperties()
	 */
	public Properties getConfigurationProperties() {
		return configurationProperties;
	}

	public String toString() {
		return toString;
	}


	protected File createTempDir(String suffix) {
		if (suffix == null)
			suffix = "osgi";
		File tempFileName;

		try {
			tempFileName = File.createTempFile("org.springframework.osgi", suffix);
		}
		catch (IOException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Could not create temporary directory, returning current folder", ex);
			}
			return new File(".");
		}

		tempFileName.delete(); // we want it to be a directory...
		File tempFolder = new File(tempFileName.getAbsolutePath());
		tempFolder.mkdir();
		return tempFolder;
	}
}
