/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.web.extender.internal.scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Scanner for Web application ARchives (WARs). This implementation simply
 * checks the <tt>.war</tt> extension of a bundle to identify a WAR.
 * 
 * @author Costin Leau
 */
public class DefaultWarScanner implements WarScanner {

	/** war extension */
	private static final String WAR_EXT = ".war";

	/** logger */
	private static final Log log = LogFactory.getLog(DefaultWarScanner.class);


	public boolean isWar(Bundle bundle) {
		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("Scanning bundle " + OsgiStringUtils.nullSafeSymbolicName(bundle));

		if (bundle == null)
			return false;

		// check bundle extension
		String location = bundle.getLocation();
		if (location != null) {
			if (trace)
				log.trace("Scanning for war bundle location " + location);
			return location.endsWith(WAR_EXT);
		}
		return false;
	}
}
