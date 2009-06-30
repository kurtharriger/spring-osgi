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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.Constants;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.extender.support.internal.ConfigUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * RFC124-version of {@link ConfigUtils} class. Basically a small util class that handles the retrieval of relevant
 * headers from the any given bundle.
 * 
 * @author Costin Leau
 * 
 */
public class BlueprintConfigUtils {

	private static final String EQUALS = "=";

	private static final String SEMI_COLON = ";";

	private static final String COMMA = ",";

	/** Manifest entry name for configuring Blueprint modules */
	public static final String BLUEPRINT_HEADER = "Bundle-Blueprint";

	/** Standard wait for dependencies header */
	public static final String BLUEPRINT_GRACE_PERIOD = "blueprint.graceperiod";
	/** Standard timeout header */
	public static final String BLUEPRINT_TIMEOUT = "blueprint.timeout";

	public static final String EXTENDER_VERSION = "BlueprintExtender-Version";

	/**
	 * Returns the {@value #BLUEPRINT_HEADER} if present from the given dictionary.
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

	public static String getSymNameHeader(Dictionary headers) {
		Object header = null;
		if (headers != null)
			header = headers.get(Constants.BUNDLE_SYMBOLICNAME);
		return (header != null ? header.toString().trim() : null);
	}

	/**
	 * Shortcut method to retrieve directive values. Used internally by the dedicated getXXX.
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
	 * Shortcut method for retrieving the directive values. Different then
	 * {@link #getDirectiveValue(Dictionary, String)} since it looks at the Bundle-Symbolic header and not at
	 * Spring-Context.
	 * 
	 * @param headers
	 * @param directiveName
	 * @return
	 */
	private static String getBlueprintDirectiveValue(Dictionary headers, String directiveName) {
		String header = getSymNameHeader(headers);
		if (header != null) {
			String directive = ConfigUtils.getDirectiveValue(header, directiveName);
			if (directive != null)
				return directive;
		}
		return null;
	}

	public static boolean hasTimeout(Dictionary headers) {
		String header = getSymNameHeader(headers);
		if (header != null) {
			return StringUtils.hasText(ConfigUtils.getDirectiveValue(header, BLUEPRINT_TIMEOUT));
		}
		return false;
	}

	/**
	 * Shortcut for finding the boolean value for {@link #BLUEPRINT_TIMEOUT} directive using the given headers.
	 * 
	 * Assumes the headers belong to a Spring powered bundle. Returns the timeout (in seconds) for which the application
	 * context should wait to have its dependencies satisfied.
	 * 
	 * @param headers
	 * @return
	 */
	public static long getTimeOut(Dictionary headers) {
		String value = getBlueprintDirectiveValue(headers, BLUEPRINT_TIMEOUT);

		if (value != null) {
			if (ConfigUtils.DIRECTIVE_TIMEOUT_VALUE_NONE.equalsIgnoreCase(value)) {
				return ConfigUtils.DIRECTIVE_NO_TIMEOUT;
			}
			return Long.valueOf(value).longValue();
		}

		return ConfigUtils.DIRECTIVE_TIMEOUT_DEFAULT;
	}

	/**
	 * Shortcut for finding the boolean value for {@link #BLUEPRINT_GRACE_PERIOD} directive using the given headers.
	 * Assumes the headers belong to a Spring powered bundle.
	 * 
	 * @param headers
	 * @return
	 */
	public static boolean getWaitForDependencies(Dictionary headers) {
		String value = getBlueprintDirectiveValue(headers, BLUEPRINT_GRACE_PERIOD);

		return (value != null ? Boolean.valueOf(value).booleanValue() : ConfigUtils.DIRECTIVE_WAIT_FOR_DEPS_DEFAULT);
	}

	/**
	 * Shortcut for finding the boolean value for {@link #DIRECTIVE_PUBLISH_CONTEXT} directive using the given headers.
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
	 * Shortcut for finding the boolean value for {@link #DIRECTIVE_CREATE_ASYNCHRONOUSLY} directive using the given
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
	 * Returns the location headers (if any) specified by the Blueprint-Bundle header (if available). The returned
	 * Strings can be sent to a {@link org.springframework.core.io.ResourceLoader} for loading the configurations.
	 * 
	 * Different from {@link ConfigUtils#getLocationsFromHeader(String, String)} since "," is used for separating clauses
	 * while ; is used inside a clause to allow parameters or directives besides paths.
	 * 
	 * @param headers bundle headers
	 * @return array of locations specified (if any)
	 */
	public static String[] getBlueprintHeaderLocations(Dictionary headers) {
		String header = getBlueprintHeader(headers);

		List<String> ctxEntries = new ArrayList<String>(4);
		if (StringUtils.hasText(header)) {
			String[] clauses = header.split(COMMA);
			for (String clause : clauses) {
				// split into directives
				String[] directives = clause.split(SEMI_COLON);
				if (!ObjectUtils.isEmpty(directives)) {
					// check if it's a path or not
					for (String directive : directives) {
						if (!directive.contains(EQUALS)) {
							ctxEntries.add(directive.trim());
						}
					}
				}
			}
		}

		// replace * with a 'digestable' location
		for (int i = 0; i < ctxEntries.size(); i++) {
			String ctxEntry = ctxEntries.get(i);
			if (ConfigUtils.CONFIG_WILDCARD.equals(ctxEntry))
				ctxEntry = OsgiBundleXmlApplicationContext.DEFAULT_CONFIG_LOCATION;
		}

		return (String[]) ctxEntries.toArray(new String[ctxEntries.size()]);
	}
}