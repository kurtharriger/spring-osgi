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

package org.springframework.osgi.extender.internal.activator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.springframework.osgi.extender.support.internal.ConfigUtils;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * @author Costin Leau
 */
public class DefaultVersionMatcher implements VersionMatcher {

	/** logger */
	private static final Log log = LogFactory.getLog(LifecycleManager.class);

	private final String versionHeader;
	private final Version expectedVersion;


	public DefaultVersionMatcher(String versionHeader, Version expectedVersion) {
		this.versionHeader = versionHeader;
		this.expectedVersion = expectedVersion;
	}

	public boolean matchVersion(Bundle bundle) {

		if (!ConfigUtils.matchExtenderVersionRange(bundle, versionHeader, expectedVersion)) {
			if (log.isDebugEnabled())
				log.debug("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle)
						+ "] expects an extender w/ version[" + bundle.getHeaders().get(versionHeader)
						+ "] which does not match current extender w/ version[" + expectedVersion
						+ "]; skipping bundle analysis...");
			return false;
		}

		return true;
	}
}
