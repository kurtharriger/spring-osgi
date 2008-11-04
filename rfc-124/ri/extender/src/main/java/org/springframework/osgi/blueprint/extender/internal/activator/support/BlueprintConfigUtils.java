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

import java.util.Dictionary;

import org.springframework.osgi.extender.support.internal.ConfigUtils;

/**
 * RFC124-version of {@link ConfigUtils} class. Basically a small util class
 * that handles the retrieval of relevant headers from the any given bundle.
 * 
 * @author Costin Leau
 * 
 */
public class BlueprintConfigUtils {

	/** Manifest entry name for configuring Blueprint modules */
	public static final String BLUEPRINT_HEADER = "Bundle-Blueprint";

	/** Standard wait for dependencies header */
	public static final String BLUEPRINT_WAIT_FOR_DEPS = "blueprint.wait-for-dependencies";
	/** Standard timeout header */
	public static final String BLUEPRINT_TIMEOUT = "blueprint.timeout";

	public static final String EXTENDER_VERSION = "BlueprintExtender-Version";


	/**
	 * Returns the {@value #BLUEPRINT_HEADER} if present from the given
	 * dictionary.
	 * 
	 * @param headers
	 * @return
	 */
	public static String getBlueprintHeader(Dictionary headers) {
		Object header = null;
		if (headers != null)
			header = headers.get(BLUEPRINT_HEADER);
		return (header != null ? header.toString().trim() : null);
	}

	/**
	 * Shortcut method to retrieve directive values. Used internally by the
	 * dedicated getXXX.
	 * 
	 * @param directiveName
	 * @return
	 */
	private static String getDirectiveValue(Dictionary headers, String directiveName) {
		String header = getBlueprintHeader(headers);
		if (header != null) {
			String directive = ConfigUtils.getDirectiveValue(header, directiveName);
			if (directive != null)
				return directive;
		}
		return null;
	}

	/**
	 * Shortcut for finding the boolean value for {@link #BLUEPRINT_TIMEOUT}
	 * directive using the given headers.
	 * 
	 * Assumes the headers belong to a Spring powered bundle. Returns the
	 * timeout (in seconds) for which the application context should wait to
	 * have its dependencies satisfied.
	 * 
	 * @param headers
	 * @return
	 */
	public static long getTimeOut(Dictionary headers) {
		String value = getDirectiveValue(headers, BLUEPRINT_TIMEOUT);

		if (value != null) {
			if (ConfigUtils.DIRECTIVE_TIMEOUT_VALUE_NONE.equalsIgnoreCase(value)) {
				return ConfigUtils.DIRECTIVE_NO_TIMEOUT;
			}
			return Long.valueOf(value).longValue();
		}

		return ConfigUtils.DIRECTIVE_TIMEOUT_DEFAULT;
	}

	/**
	 * Shortcut for finding the boolean value for
	 * {@link #BLUEPRINT_WAIT_FOR_DEPS} directive using the given headers.
	 * Assumes the headers belong to a Spring powered bundle.
	 * 
	 * @param headers
	 * @return
	 */
	public static boolean getWaitForDependencies(Dictionary headers) {
		String value = getDirectiveValue(headers, BLUEPRINT_WAIT_FOR_DEPS);

		return (value != null ? Boolean.valueOf(value).booleanValue() : ConfigUtils.DIRECTIVE_WAIT_FOR_DEPS_DEFAULT);
	}

	/**
	 * Shortcut for finding the boolean value for
	 * {@link #DIRECTIVE_PUBLISH_CONTEXT} directive using the given headers.
	 * Assumes the headers belong to a Spring powered bundle.
	 * 
	 * @param headers
	 * @return
	 */
	public static boolean getPublishContext(Dictionary headers) {
		String value = getDirectiveValue(headers, ConfigUtils.DIRECTIVE_PUBLISH_CONTEXT);
		return (value != null ? Boolean.valueOf(value).booleanValue() : ConfigUtils.DIRECTIVE_PUBLISH_CONTEXT_DEFAULT);
	}

	/**
	 * Shortcut for finding the boolean value for
	 * {@link #DIRECTIVE_CREATE_ASYNCHRONOUSLY} directive using the given
	 * headers.
	 * 
	 * Assumes the headers belong to a Spring powered bundle.
	 * 
	 * @param headers
	 * @return
	 */
	public static boolean getCreateAsync(Dictionary headers) {
		String value = getDirectiveValue(headers, ConfigUtils.DIRECTIVE_CREATE_ASYNCHRONOUSLY);
		return (value != null ? Boolean.valueOf(value).booleanValue()
				: ConfigUtils.DIRECTIVE_CREATE_ASYNCHRONOUSLY_DEFAULT);
	}

	/**
	 * Returns the location headers (if any) specified by the Blueprint-Bundle
	 * header (if available). The returned Strings can be sent to a
	 * {@link org.springframework.core.io.ResourceLoader} for loading the
	 * configurations.
	 * 
	 * @param headers bundle headers
	 * @return array of locations specified (if any)
	 */
	public static String[] getHeaderLocations(Dictionary headers) {
		return ConfigUtils.getLocationsFromHeader(getBlueprintHeader(headers),
			BlueprintConfigurationScanner.DEFAULT_CONFIG);
	}
}
