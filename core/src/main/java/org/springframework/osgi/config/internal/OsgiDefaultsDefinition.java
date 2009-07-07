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

package org.springframework.osgi.config.internal;

import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.config.internal.util.ReferenceParsingUtil;
import org.springframework.osgi.service.importer.support.Availability;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class containing osgi defaults.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiDefaultsDefinition {

	private static final String OSGI_NS = "http://www.springframework.org/schema/osgi";

	private static final String DEFAULT_TIMEOUT = "default-timeout";
	private static final String DEFAULT_AVAILABILITY = "default-availability";
	private static final String DEFAULT_CARDINALITY = "default-cardinality";
	private static final String TIMEOUT_DEFAULT = "300000";

	/** Default value */
	private String timeout = TIMEOUT_DEFAULT;
	/** Default value */
	private Availability availability = Availability.MANDATORY;

	public OsgiDefaultsDefinition(Document document, ParserContext parserContext) {
		Assert.notNull(document);
		Element root = document.getDocumentElement();

		ReferenceParsingUtil.checkAvailabilityAndCardinalityDuplication(root, DEFAULT_AVAILABILITY,
				DEFAULT_CARDINALITY, parserContext);

		String timeout = getAttribute(root, OSGI_NS, DEFAULT_TIMEOUT);

		if (StringUtils.hasText(timeout)) {
			setTimeout(timeout);
		}

		String availability = getAttribute(root, OSGI_NS, DEFAULT_AVAILABILITY);

		if (StringUtils.hasText(availability)) {
			setAvailability(ReferenceParsingUtil.determineAvailability(availability));
		}

		String cardinality = getAttribute(root, OSGI_NS, DEFAULT_CARDINALITY);

		if (StringUtils.hasText(cardinality)) {
			setAvailability(ReferenceParsingUtil.determineAvailabilityFromCardinality(cardinality));
		}
	}

	public String getTimeout() {
		return timeout;
	}

	protected void setTimeout(String timeout) {
		this.timeout = timeout;
	}

	public Availability getAvailability() {
		return availability;
	}

	protected void setAvailability(Availability availability) {
		this.availability = availability;
	}

	protected String getAttribute(Element root, String ns, String attributeName) {
		String value = root.getAttributeNS(ns, attributeName);
		return (!StringUtils.hasText(value) ? root.getAttribute(attributeName) : value);
	}
}