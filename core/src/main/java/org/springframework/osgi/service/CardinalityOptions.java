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
package org.springframework.osgi.service;

import org.springframework.core.Constants;
import org.springframework.util.Assert;

/**
 * Cardinality constants class.
 * 
 * @author Costin Leau
 * 
 */
public abstract class CardinalityOptions {

	public static final int C_0__1 = 0;

	public static final int C_0__N = 1;

	public static final int C_1__1 = 2;

	public static final int C_1__N = 3;

	private final static Constants CARDINALITY = new Constants(CardinalityOptions.class);

	/**
	 * Translates the cardinality string into an int representation. Accepts
	 * values 0..N, 1..N, 0..1, 1..1.
	 * 
	 * @param cardinality
	 * @return
	 */
	public static int asInt(String cardinality) {
		Assert.hasText(cardinality);

		// transform string to the constant representation
		// a. C_ is appended to the string
		// b. . -> _
		return CARDINALITY.asNumber("C_".concat(cardinality.replace('.', '_'))).intValue();
	}

	public static boolean atMostOneExpected(int c) {
		return CardinalityOptions.C_0__1 == c || CardinalityOptions.C_1__1 == c;
	}

	public static boolean atLeastOneRequired(int c) {
		return CardinalityOptions.C_1__1 == c || CardinalityOptions.C_1__N == c;
	}

	public static boolean moreThanOneExpected(int c) {
		return CardinalityOptions.C_0__N == c || CardinalityOptions.C_1__N == c;
	}
}
