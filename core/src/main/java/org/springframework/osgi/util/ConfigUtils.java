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
package org.springframework.osgi.util;

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.springframework.osgi.io.OsgiBundleResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * OSGi bundle manifest manifest based utility class.
 * 
 * Defines Spring/OSGi constants and method for configuring Spring application
 * context.
 * 
 * @author Costin Leau
 * 
 */
public abstract class ConfigUtils {

	private static final Log log = LogFactory.getLog(ConfigUtils.class);

	protected static final String CONTEXT_DIR = "/META-INF/spring/";

	protected static final String CONTEXT_FILES = "*.xml";

	protected static final String META_INF_WILD_CARD = CONTEXT_DIR + CONTEXT_FILES;

	public static final String SPRING_CONTEXT_FILES = OsgiBundleResource.BUNDLE_JAR_URL_PREFIX + META_INF_WILD_CARD;

	public static final String CONFIG_WILDCARD = "*";

	/**
	 * Manifest entry name for configuring Spring application context.
	 */
	public static final String SPRING_CONTEXT_HEADER = "Spring-Context";

	/**
	 * Directive for publishing Spring application context as a service.
	 */
	public static final String DIRECTIVE_PUBLISH_CONTEXT = "publish-context";

	/**
	 * Directive for indicating wait-for time when satisfying manditory
	 * dependencies defined in seconds
	 */
	public static final String DIRECTIVE_TIMEOUT = "timeout";

	public static final String DIRECTIVE_TIMEOUT_VALUE_NONE = "none";

	/**
	 * Create asynchronously directive.
	 */
	public static final String DIRECTIVE_CREATE_ASYNCHRONOUSLY = "create-asynchronously";

	/**
	 * Wait for dependencies.
	 */
	public static final String DIRECTIVE_WAIT_FOR_DEPS = "wait-for-dependencies";

	public static final String EQUALS = ":=";

	/**
	 * Token used for separating directives inside a header.
	 */
	public static final String DIRECTIVE_SEPARATOR = ";";

	public static final String CONTEXT_LOCATION_SEPARATOR = ",";

	public static final boolean DIRECTIVE_PUBLISH_CONTEXT_DEFAULT = true;

	public static final boolean DIRECTIVE_CREATE_ASYNCHRONOUSLY_DEFAULT = true;

	public static final long DIRECTIVE_TIMEOUT_DEFAULT = 5 * 60; // 5 minutes

	public static final long DIRECTIVE_NO_TIMEOUT = -2L; // Indicates wait

	// forever

	/**
	 * Return the {@value #SPRING_CONTEXT_HEADER} if present from the given
	 * dictionary.
	 * 
	 * @param headers
	 * @return
	 */
	public static String getSpringContextHeader(Dictionary headers) {
		Object header = null;
		if (headers != null)
			header = headers.get(SPRING_CONTEXT_HEADER);
		return (header != null ? header.toString() : null);
	}

	/**
	 * Return the directive value as a String. If the directive does not exist
	 * or is invalid (wrong format) a null string will be returned.
	 * 
	 * @param header
	 * @param directive
	 * @return
	 */
	public static String getDirectiveValue(String header, String directive) {
		Assert.notNull(header, "not-null header required");
		Assert.notNull(directive, "not-null directive required");
		String[] directives = StringUtils.tokenizeToStringArray(header, DIRECTIVE_SEPARATOR);

		for (int i = 0; i < directives.length; i++) {
			String[] splittedDirective = StringUtils.delimitedListToStringArray(directives[i].trim(), EQUALS);
			if (splittedDirective.length == 2 && splittedDirective[0].equals(directive))
				return splittedDirective[1];
		}

		return null;
	}

	/**
	 * Shortuct method to retrieve directive values. Used internally by the
	 * dedicated getXXX.
	 * 
	 * @param directiveName
	 * @return
	 */
	private static String getDirectiveValue(Dictionary headers, String directiveName) {
		String header = getSpringContextHeader(headers);
		if (header != null) {
			String directive = getDirectiveValue(header, directiveName);
			if (directive != null)
				return directive;
		}
		return null;
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
		String value = getDirectiveValue(headers, DIRECTIVE_PUBLISH_CONTEXT);
		return (value != null ? Boolean.valueOf(value).booleanValue() : DIRECTIVE_PUBLISH_CONTEXT_DEFAULT);
	}

	/**
	 * Shortcut for finding the boolean value for
	 * {@link #DIRECTIVE_CREATE_ASYNCHRONOUSLY} directive using the given
	 * headers. Assumes the headers belong to a Spring powered bundle.
	 * 
	 * @param headers
	 * @return
	 */
	public static boolean getCreateAsync(Dictionary headers) {
		String value = getDirectiveValue(headers, DIRECTIVE_CREATE_ASYNCHRONOUSLY);
		return (value != null ? Boolean.valueOf(value).booleanValue() : DIRECTIVE_CREATE_ASYNCHRONOUSLY_DEFAULT);
	}

	/**
	 * Shortcut for finding the boolean value for {@link #DIRECTIVE_TIMEOUT}
	 * directive using the given headers. Assumes the headers belong to a Spring
	 * powered bundle.
	 * 
	 * @param headers
	 * @return
	 */
	public static long getTimeOut(Dictionary headers) {
		String value = getDirectiveValue(headers, DIRECTIVE_TIMEOUT);

		if (value != null) {
			if (DIRECTIVE_TIMEOUT_VALUE_NONE.equalsIgnoreCase(value)) {
				return DIRECTIVE_NO_TIMEOUT;
			}
			return Long.valueOf(value).longValue();
		}

		return DIRECTIVE_TIMEOUT_DEFAULT;
	}

	/**
	 * Return the config locations from the Spring-Context header. The returned
	 * Strings can be sent to a
	 * {@link org.springframework.core.io.ResourceLoader} for loading the
	 * configurations.
	 * 
	 * <p/> This method does not perform any validation of the given resources.
	 * 
	 * @param headers
	 * @return
	 */
	public static String[] getConfigLocations(Dictionary headers) {
		String header = getSpringContextHeader(headers);
		if (header != null) {
			header = header.trim();
		}

		String[] ctxEntries;
		if (StringUtils.hasText(header) && !(';' == header.charAt(0))) {
			// get the file location
			String locations = StringUtils.tokenizeToStringArray(header, DIRECTIVE_SEPARATOR)[0];
			// parse it into individual token
			ctxEntries = StringUtils.tokenizeToStringArray(locations, CONTEXT_LOCATION_SEPARATOR);

			// replace * with a 'digestable' location
			for (int i = 0; i < ctxEntries.length; i++) {
				if (CONFIG_WILDCARD.equals(ctxEntries[i]))
					ctxEntries[i] = SPRING_CONTEXT_FILES;
			}
		}
		else {
			ctxEntries = new String[] { SPRING_CONTEXT_FILES };
		}

		// remove duplicates
		return StringUtils.removeDuplicateStrings(ctxEntries);
	}

	/**
	 * Dictates if the given bundle is Spring/OSGi powered or not.
	 * 
	 * <p/> The method looks first for a Spring-Context bundle header and then
	 * for *.xml files under META-INF/spring folder. If either is present, true
	 * is returned.
	 * 
	 * @param bundle
	 * @return
	 */
	public static boolean isSpringOsgiPoweredBundle(Bundle bundle) {
		Assert.notNull(bundle, "non-null bundle required");

		// look first for Spring-Context entry
		if (getSpringContextHeader(bundle.getHeaders()) != null)
			return true;

		// check the default locations now (could use the IO to do the checking)
		Enumeration defaultConfig = bundle.findEntries(CONTEXT_DIR, CONTEXT_FILES, false);
		return (defaultConfig != null && defaultConfig.hasMoreElements());
	}
}
