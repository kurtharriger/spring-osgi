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
package org.springframework.osgi.context.support;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.xml.DefaultDocumentLoader;
import org.springframework.beans.factory.xml.DocumentLoader;

/**
 * Specialized {@link DocumentLoader} that allows blueprint configurations without a schema location to be properly
 * validated.
 * 
 * @author Costin Leau
 */
class BlueprintDocumentLoader extends DefaultDocumentLoader {

	static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	static final String BLUEPRINT_SCHEMA = "http://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd";

	/** logger */
	private static final Log log = LogFactory.getLog(BlueprintDocumentLoader.class);

	@Override
	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
			throws ParserConfigurationException {
		DocumentBuilderFactory factory = super.createDocumentBuilderFactory(validationMode, namespaceAware);
		try {
			factory.setAttribute(JAXP_SCHEMA_SOURCE, BLUEPRINT_SCHEMA);
		} catch (IllegalArgumentException ex) {
			log.warn("Cannot work with attribute " + JAXP_SCHEMA_SOURCE
					+ " - configurations w/o a schema locations will likely fail to validate", ex);
		}

		return factory;
	}
}
