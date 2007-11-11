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
package org.springframework.osgi.service.importer.support;

import org.springframework.core.enums.StaticLabeledEnum;

/**
 * Imported OSGi services cardinality. Indicates the number of expected services
 * and whether the presence is mandatory or not.
 * 
 * @author Costin Leau
 * 
 */
public class Cardinality extends StaticLabeledEnum {

	private static final long serialVersionUID = 6377096464873348405L;

	/**
	 * Optional, single cardinality.
	 */
	public static final Cardinality C_0__1 = new Cardinality(0, "0..1");

	/**
	 * Optional, multiple cardinality.
	 */
	public static final Cardinality C_0__N = new Cardinality(1, "0..N");

	/**
	 * Mandatory, single cardinality.
	 */
	public static final Cardinality C_1__1 = new Cardinality(2, "1..1");

	/**
	 * Mandatory, multiple cardinality.
	 */
	public static final Cardinality C_1__N = new Cardinality(3, "1..N");

	/**
	 * Does this cardinality indicate that at most one service is expected?
	 * 
	 * @param cardinality cardinality object
	 * @return true if the given cardinality is single, false otherwise
	 */
	public static boolean isSingle(Cardinality cardinality) {
		return Cardinality.C_0__1.equals(cardinality) || Cardinality.C_1__1.equals(cardinality);
	}

	/**
	 * Does this cardinality indicate that multiple services are expected?
	 * 
	 * @param cardinality cardinality object
	 * @return true if the given cardinality is multiple, false otherwise
	 */
	public static boolean isMultiple(Cardinality cardinality) {
		return Cardinality.C_0__N.equals(cardinality) || Cardinality.C_1__N.equals(cardinality);
	}

	/**
	 * Does this cardinality indicate that at least one service is required
	 * (mandatory cardinality).
	 * 
	 * @param cardinality cardinality object
	 * @return true if the given cardinality is mandatory, false otherwise
	 */
	public static boolean isMandatory(Cardinality cardinality) {
		return Cardinality.C_1__1.equals(cardinality) || Cardinality.C_1__N.equals(cardinality);
	}

	/**
	 * Does this cardinality indicate that no service found is acceptable?
	 * 
	 * @param cardinality cardinality object
	 * @return true if the given cardinality is optional, false otherwise
	 */
	public static boolean isOptional(Cardinality cardinality) {
		return Cardinality.C_0__N.equals(cardinality) || Cardinality.C_0__1.equals(cardinality);
	}

	/**
	 * Constructs a new <code>Cardinality</code> instance.
	 * 
	 * @param code
	 * @param label
	 */
	private Cardinality(int code, String label) {
		super(code, label);
	}
}
