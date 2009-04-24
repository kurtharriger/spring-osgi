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

	private static final String DEFAULT_CARDINALITY = "default-cardinality";

	private static final String TIMEOUT_DEFAULT = "300000";

	private static final String CARDINALITY_DEFAULT = "1..X";

	/** Default value */
	private String timeout = TIMEOUT_DEFAULT;

	/** Default value */
	private String cardinality = CARDINALITY_DEFAULT;


	public OsgiDefaultsDefinition(Document document) {
		Assert.notNull(document);
		Element root = document.getDocumentElement();

		String timeout = root.getAttributeNS(OSGI_NS, DEFAULT_TIMEOUT);

		if (StringUtils.hasText(timeout))
			setTimeout(timeout);

		String cardinality = root.getAttributeNS(OSGI_NS, DEFAULT_CARDINALITY);

		if (StringUtils.hasText(cardinality))
			setCardinality(cardinality);
	}

	public String getTimeout() {
		return timeout;
	}

	protected void setTimeout(String timeout) {
		this.timeout = timeout;
	}

	public String getCardinality() {
		return cardinality;
	}

	protected void setCardinality(String cardinality) {
		this.cardinality = cardinality;
	}
}
