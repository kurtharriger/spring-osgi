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

import java.net.URL;

import org.osgi.framework.Bundle;

/**
 * War scanner. Detects the presence of <code>web.xml</code> files or other
 * war configurations.
 * 
 * @author Costin Leau
 * 
 */
public interface WarScanner {

	/**
	 * Returns the <code>web.xml</code> configuration (if it exists) for the
	 * given bundle.
	 * 
	 * @param bundle OSGi bundle
	 * @return URL to the standard web.xml configuration
	 */
	URL getWebXmlConfiguration(Bundle bundle);
}
