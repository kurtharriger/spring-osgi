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

package org.springframework.osgi.blueprint.extender.internal.activator.support;

import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.springframework.osgi.extender.support.internal.ConfigUtils;
import org.springframework.osgi.extender.support.scanning.ConfigurationScanner;
import org.springframework.osgi.io.OsgiBundleResource;
import org.springframework.util.ObjectUtils;

/**
 * Dedication {@link ConfigurationScanner scanner} implementation suitable for
 * RFC 124 files.
 * 
 * @author Costin Leau
 * 
 */
public class BlueprintConfigurationScanner implements ConfigurationScanner {

	private static final String CONTEXT_DIR = "OSGI-INF/blueprint/";

	private static final String CONTEXT_FILES = "*.xml";

	/** Default configuration location */
	public static final String DEFAULT_CONFIG = OsgiBundleResource.BUNDLE_URL_PREFIX + CONTEXT_DIR + CONTEXT_FILES;


	public String[] getConfigurations(Bundle bundle) {
		String[] locations = BlueprintConfigUtils.getHeaderLocations(bundle.getHeaders());

		// if no location is specified in the header, try the defaults
		if (ObjectUtils.isEmpty(locations)) {
			// check the default locations if the manifest doesn't provide any info
			Enumeration defaultConfig = bundle.findEntries(CONTEXT_DIR, CONTEXT_FILES, false);
			if (defaultConfig != null && defaultConfig.hasMoreElements()) {
				return new String[] { DEFAULT_CONFIG };
			}
			else {
				return new String[0];
			}
		}
		else {
			return locations;
		}
	}
}
