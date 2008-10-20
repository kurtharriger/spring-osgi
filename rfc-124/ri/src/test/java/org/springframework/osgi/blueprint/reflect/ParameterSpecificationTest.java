/*
 * Copyright 2008 the original author or authors.
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
package org.springframework.osgi.blueprint.reflect;

import org.junit.Test;
import org.osgi.service.blueprint.reflect.Value;
import static org.junit.Assert.*;

public class ParameterSpecificationTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullValue() {
		new PS(null);
	}
	
	@Test
	public void testNonNullValue() {
		ReferenceValueObject rv = new ReferenceValueObject("foo");
		PS p = new PS(rv);
		assertSame(rv,p.getValue());
	}
	
	// to help testing of abstract class
	private static class PS extends AbstractParameterSpecification {
		public PS(Value v) {
			super(v);
		}
	}
	
}
