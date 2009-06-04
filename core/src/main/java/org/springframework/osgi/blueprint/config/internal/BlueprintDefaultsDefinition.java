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

package org.springframework.osgi.blueprint.config.internal;

import org.springframework.osgi.config.internal.OsgiDefaultsDefinition;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Blueprint namespace defaults for a given element/document.
 * 
 * @author Costin Leau
 * 
 */
public class BlueprintDefaultsDefinition extends OsgiDefaultsDefinition {

	private static final String BLUEPRINT_NS = "http://www.osgi.org/xmlns/blueprint/v1.0.0";
	private static final String DEFAULT_TIMEOUT = "default-timeout";
	private static final String DEFAULT_CARDINALITY = "default-availability";
	private static final String TIMEOUT_DEFAULT = "300000";
	private static final String CARDINALITY_DEFAULT = "mandatory";
	private static final String DEFAULT_INITIALIZATION = "default-initialization";
	private static final String LAZY_INITIALIZATION = "lazy";
	private static final boolean INITIALIZATION_DEFAULT = false;

	private boolean defaultInitialization;

	/**
	 * Constructs a new <code>BlueprintDefaultsDefinition</code> instance.
	 * 
	 * @param root
	 */
	public BlueprintDefaultsDefinition(Document doc) {
		super(doc);
		Element root = doc.getDocumentElement();
		String timeout = root.getAttributeNS(BLUEPRINT_NS, DEFAULT_TIMEOUT);
		setTimeout(StringUtils.hasText(timeout) ? timeout.trim() : TIMEOUT_DEFAULT);
		// cardinality
		String cardinality = root.getAttributeNS(BLUEPRINT_NS, DEFAULT_CARDINALITY);
		setCardinality(StringUtils.hasText(cardinality) ? cardinality.trim() : CARDINALITY_DEFAULT);
		// default initialization
		String initialization = root.getAttributeNS(BLUEPRINT_NS, DEFAULT_INITIALIZATION);
		defaultInitialization =
				(StringUtils.hasText(initialization) ? initialization.trim().equalsIgnoreCase(LAZY_INITIALIZATION)
						: INITIALIZATION_DEFAULT);
	}

	public boolean getDefaultInitialization() {
		return defaultInitialization;
	}
}
