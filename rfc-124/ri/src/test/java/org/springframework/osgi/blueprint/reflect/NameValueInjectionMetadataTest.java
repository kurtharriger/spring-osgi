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
import static org.junit.Assert.*;

public class NameValueInjectionMetadataTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullName() {
		new MutableFieldInjectionMetadata(null,new ReferenceValueObject("ff"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullValue() {
		new MutableFieldInjectionMetadata("foo",null);
	}
	
	@Test
	public void testGetName() {
		NameValueInjectionMetadata fim = new MutableFieldInjectionMetadata("foo",new ReferenceValueObject("foo"));
		assertEquals("foo",fim.getName());
	}
	
	@Test
	public void testGetValue() {
		ReferenceValueObject rv = new ReferenceValueObject("foo");
		NameValueInjectionMetadata fim = new MutableFieldInjectionMetadata("foo",rv);
		assertSame(rv,fim.getValue());
	}
	
	@Test
	public void testSetValue() {
		ReferenceValueObject rv = new ReferenceValueObject("foo");
		NameValueInjectionMetadata fim = new MutableFieldInjectionMetadata("foo",rv);
		ReferenceValueObject rv2 = new ReferenceValueObject("bar");
		fim.setValue(rv2);
		assertSame(rv2,fim.getValue());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetNullValue() {
		ReferenceValueObject rv = new ReferenceValueObject("foo");
		NameValueInjectionMetadata fim = new MutableFieldInjectionMetadata("foo",rv);
		fim.setValue(null);
	}
}
