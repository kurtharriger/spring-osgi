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
package org.springframework.osgi.internal.config;

/**
 * Class containing osgi defaults.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiDefaultsDefinition {

	// TODO: would be nice to be able to read the defaults 
	// from the schema directly. This seems impossible since
	// the osgi attribute is not declared on the beans
	// root element which has no idea about the schema or import
	
	private static final String TIMEOUT_DEFAULT = "1000";
	
	private static final String CARDINALITY_DEFAULT = "1..X";
	
	/** Default value */
	private String timeout = TIMEOUT_DEFAULT;

	/** Default value */
	private String cardinality = CARDINALITY_DEFAULT;

	public String getTimeout() {
		return timeout;
	}

	public void setTimeout(String timeout) {
		this.timeout = timeout;
	}

	public String getCardinality() {
		return cardinality;
	}

	public void setCardinality(String cardinality) {
		this.cardinality = cardinality;
	}

}
