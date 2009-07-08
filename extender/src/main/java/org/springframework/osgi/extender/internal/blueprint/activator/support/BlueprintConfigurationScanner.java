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

package org.springframework.osgi.extender.internal.blueprint.activator.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.osgi.extender.support.scanning.ConfigurationScanner;
import org.springframework.osgi.io.OsgiBundleResource;
import org.springframework.osgi.io.OsgiBundleResourcePatternResolver;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.ObjectUtils;

/**
 * Dedication {@link ConfigurationScanner scanner} implementation suitable for Blueprint bundles.
 * 
 * @author Costin Leau
 */
public class BlueprintConfigurationScanner implements ConfigurationScanner {

	/** logger */
	private static final Log log = LogFactory.getLog(BlueprintConfigurationScanner.class);

	private static final String CONTEXT_DIR = "OSGI-INF/blueprint/";

	private static final String CONTEXT_FILES = "*.xml";

	/** Default configuration location */
	public static final String DEFAULT_CONFIG = OsgiBundleResource.BUNDLE_URL_PREFIX + CONTEXT_DIR + CONTEXT_FILES;

	public String[] getConfigurations(Bundle bundle) {
		String bundleName = OsgiStringUtils.nullSafeName(bundle);

		boolean trace = log.isTraceEnabled();
		boolean debug = log.isDebugEnabled();

		if (debug)
			log.debug("Scanning bundle " + bundleName + " for blueprint configurations...");

		String[] locations = BlueprintConfigUtils.getBlueprintHeaderLocations(bundle.getHeaders());

		// if no location is specified in the header, try the defaults
		if (locations == null) {
			if (trace) {
				log.trace("Bundle " + bundleName + " has no declared locations; trying default " + DEFAULT_CONFIG);
			}
			locations = new String[] { DEFAULT_CONFIG };
		} else {
			// check whether the header is empty
			if (ObjectUtils.isEmpty(locations)) {
				log.info("Bundle " + bundleName + " has an empty blueprint header - ignoring bundle...");
				return new String[0];
			}
		}

		System.out.println("About to validate locations " + Arrays.toString(locations));
		String[] configs = findValidBlueprintConfigs(bundle, locations);
		if (debug)
			log.debug("Discovered in bundle" + bundleName + " blueprint configurations=" + Arrays.toString(configs));
		return configs;
	}

	/**
	 * Checks if the given bundle contains existing configurations. The absolute paths are returned without performing
	 * any checks.
	 * 
	 * @return
	 */
	private String[] findValidBlueprintConfigs(Bundle bundle, String[] locations) {
		List<String> configs = new ArrayList<String>(locations.length);
		ResourcePatternResolver loader = new OsgiBundleResourcePatternResolver(bundle);

		boolean debug = log.isDebugEnabled();
		for (String location : locations) {
			System.out.println("Looking at location " + location);
			if (isAbsolute(location)) {
				configs.add(location);
				System.out.println("Location " + location + " is absolute; adding it to the list");
			}
			// resolve the location to check if it's present
			else {
				try {
					String loc = location;
					if (loc.endsWith("/")) {
						System.out.println("Location " + loc + " is a folder; looking for xml files in it...");
						loc = loc + "*.xml";
					}
					Resource[] resources = loader.getResources(loc);
					System.out.println("Retrieved locations for location " + loc + " = " + Arrays.toString(resources));
					if (!ObjectUtils.isEmpty(resources)) {
						for (Resource resource : resources) {
							if (resource.exists()) {
								String value = resource.getURL().toString();
								if (debug)
									log.debug("Found location " + value);
								System.out.println("Found location " + value);
								configs.add(value);
							}
						}
					}
				} catch (IOException ex) {
					if (debug)
						log.debug("Cannot resolve location " + location, ex);
				}
			}
		}
		return (String[]) configs.toArray(new String[configs.size()]);
	}

	private boolean isAbsolute(String location) {
		return !(location.endsWith("/") || location.contains("*"));
	}
}