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
package org.springframework.osgi.service.importer;

import org.springframework.core.enums.StaticLabeledEnum;
import org.springframework.core.enums.StaticLabeledEnumResolver;

/**
 * Cardinality constants class.
 * 
 * @author Costin Leau
 * 
 */
public class CardinalityOptions extends StaticLabeledEnum {

	/**
	 * Optional, singular cardinality.
	 */
	public static final CardinalityOptions C_0__1 = new CardinalityOptions(0, "0..1");

	/**
	 * Optional, multi-cardinality.
	 */
	public static final CardinalityOptions C_0__N = new CardinalityOptions(1, "0..N");

	/**
	 * Mandatory, singular cardinality.
	 */
	public static final CardinalityOptions C_1__1 = new CardinalityOptions(2, "1..1");

	/**
	 * Mandatory, multi-cardinality.
	 */
	public static final CardinalityOptions C_1__N = new CardinalityOptions(3, "1..N");

	/**
	 * Does this cardinality indicate that at most one service is expected?
	 * 
	 * @param cardinality
	 * @return
	 */
	public static boolean isSingular(CardinalityOptions cardinality) {
		return CardinalityOptions.C_0__1.equals(cardinality) || CardinalityOptions.C_1__1.equals(cardinality);
	}
	
	/**
	 * Does this cardinality indicate that multiple services are expected?
	 * 
	 * @param cardinality
	 * @return
	 */
	public static boolean isMultiple(CardinalityOptions cardinality) {
		return CardinalityOptions.C_0__N.equals(cardinality) || CardinalityOptions.C_1__N.equals(cardinality);
	}

	/**
	 * Does this cardinality indicate that at least one service is required (mandatory cardinality).
	 * 
	 * @param cardinality
	 * @return
	 */
	public static boolean isMandatory(CardinalityOptions cardinality) {
		return CardinalityOptions.C_1__1.equals(cardinality) || CardinalityOptions.C_1__N.equals(cardinality);
	}

	/**
	 * Does this cardinality indicate that no service found is acceptable? 
	 * 
	 * @param cardinality
	 * @return
	 */
	public static boolean isOptional(CardinalityOptions cardinality) {
		return CardinalityOptions.C_0__N.equals(cardinality) || CardinalityOptions.C_0__1.equals(cardinality);
	}
	
	public static CardinalityOptions resolveEnum(String label) {
		return (CardinalityOptions) StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(CardinalityOptions.class, label);
	}

	public static CardinalityOptions resolveEnum(int code) {
		return (CardinalityOptions) StaticLabeledEnumResolver.instance().getLabeledEnumByCode(CardinalityOptions.class, new Integer(code));
	}


	
	private CardinalityOptions(int code, String label) {
		super(code, label);
	}
}
