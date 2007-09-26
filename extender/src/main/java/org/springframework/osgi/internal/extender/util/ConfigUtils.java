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
package org.springframework.osgi.internal.extender.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class for dealing with the extender configuration.
 * 
 * @author Costin Leau
 * 
 */
public abstract class ConfigUtils {

	private static final Log log = LogFactory.getLog(ConfigUtils.class);

	public static final String EXTENDER_VERSION = "SpringExtender-Version";

	private static final String LEFT_CLOSED_INTERVAL = "[";

	private static final String LEFT_OPEN_INTERVAL = "(";

	private static final String RIGHT_CLOSED_INTERVAL = "]";

	private static final String RIGHT_OPEN_INTERVAL = ")";

	private static final String COMMA = ",";

	public static boolean matchExtenderVersionRange(Bundle bundle, Version versionToMatch) {
		Assert.notNull(bundle);
		// get version range
		String range = (String) bundle.getHeaders().get(EXTENDER_VERSION);

		boolean trace = log.isTraceEnabled();

		// empty value = empty version = *
		if (!StringUtils.hasText(range))
			return true;

		if (trace)
			log.trace("discovered " + EXTENDER_VERSION + " header w/ value=" + range);

		// do we have a range or not ?
		range = StringUtils.trimWhitespace(range);

		// a range means one comma
		int commaNr = StringUtils.countOccurrencesOf(range, COMMA);

		// no comma, no intervals
		if (commaNr == 0) {
			Version version = Version.parseVersion(range);

			return versionToMatch.equals(version);
		}

		if (commaNr == 1) {

			// sanity check
			if (!((range.startsWith(LEFT_CLOSED_INTERVAL) || range.startsWith(LEFT_OPEN_INTERVAL)) && (range.endsWith(RIGHT_CLOSED_INTERVAL) || range.endsWith(RIGHT_OPEN_INTERVAL)))) {
				throw new IllegalArgumentException("range [" + range + "] is invalid");
			}

			boolean equalMin = range.startsWith(LEFT_CLOSED_INTERVAL);
			boolean equalMax = range.endsWith(RIGHT_CLOSED_INTERVAL);

			// remove interval brackets
			range = range.substring(1, range.length() - 1);

			// split the remaining string in two pieces
			String[] pieces = StringUtils.split(range, COMMA);

			if (trace)
				log.trace("discovered low/high versions : " + ObjectUtils.nullSafeToString(pieces));

			Version minVer = Version.parseVersion(pieces[0]);
			Version maxVer = Version.parseVersion(pieces[1]);

			if (trace)
				log.trace("comparing version " + versionToMatch + " w/ min=" + minVer + " and max=" + maxVer);

			boolean result = true;

			int compareMin = versionToMatch.compareTo(minVer);

			if (equalMin)
				result = (result && (compareMin >= 0));
			else
				result = (result && (compareMin > 0));

			int compareMax = versionToMatch.compareTo(maxVer);

			if (equalMax)
				result = (result && (compareMax <= 0));
			else
				result = (result && (compareMax < 0));

			return result;
		}

		// more then one comma means incorrect range

		throw new IllegalArgumentException("range [" + range + "] is invalid");
	}
}
