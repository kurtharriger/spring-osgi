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

import java.util.Properties;

/**
 * Base class for OsgiPlatform classes.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractOsgiPlatform implements OsgiPlatform {

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
}
