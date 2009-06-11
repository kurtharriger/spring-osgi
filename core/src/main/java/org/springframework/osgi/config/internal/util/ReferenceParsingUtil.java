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
package org.springframework.osgi.config.internal.util;

import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.service.importer.support.Availability;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Reference related parsing utility.
 * 
 * @author Costin Leau
 */
public abstract class ReferenceParsingUtil {

	private static final String ONE = "1";
	private static final String M = "m";

	public static void checkAvailabilityAndCardinalityDuplication(Element element, String availabilityName,
			String cardinalityName, ParserContext context) {

		String avail = element.getAttribute(availabilityName);
		String cardinality = element.getAttribute(cardinalityName);

		if (StringUtils.hasText(avail) && StringUtils.hasText(cardinality)) {
			boolean availStatus = avail.startsWith(ONE);
			boolean cardStatus = cardinality.startsWith(M);

			if (availStatus != cardStatus) {
				context.getReaderContext().error(
						"Both '" + availabilityName + "' and '" + cardinalityName
								+ "' attributes have been specified but with contradictory values.", element);
			}
		}
	}

	public static Availability determineAvailabilityFromCardinality(String value) {
		return (value.startsWith(ONE) ? Availability.MANDATORY : Availability.OPTIONAL);
	}

	public static Availability determineAvailability(String value) {
		return (value.startsWith(M) ? Availability.MANDATORY : Availability.OPTIONAL);
	}
}
