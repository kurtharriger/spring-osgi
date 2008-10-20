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
import org.osgi.service.blueprint.reflect.ParameterSpecification;

import static org.junit.Assert.*;

public class ParameterBasedInjectionMetadataTest {

	@Test
	public void testNullParamSpec() {
		ParameterBasedInjectionMetadata cim =
			new MutableConstructorInjectionMetadata(null);
		assertEquals(0,cim.getParameterSpecifications().length);
	}
	
	@Test
	public void testNonNullParamSpec() {
		ParameterSpecification[] ps = new ParameterSpecification[2];
		ps[0] = new ImmutableIndexedParameterSpecification(0,new ReferenceValueObject("foo"));
		ps[1] = new ImmutableIndexedParameterSpecification(1,new ReferenceValueObject("bar"));
		ParameterBasedInjectionMetadata cim = 
			new MutableConstructorInjectionMetadata(ps);
		assertSame(ps,cim.getParameterSpecifications());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNonNullArrayWithNullValue() {
		ParameterSpecification[] ps = new ParameterSpecification[2];
		ps[0] = new ImmutableIndexedParameterSpecification(0,new ReferenceValueObject("foo"));
		new MutableConstructorInjectionMetadata(ps);		
	}
	
	@Test
	public void testSetNullParamSpec() {
		ParameterBasedInjectionMetadata cim =
			new MutableConstructorInjectionMetadata(null);
		cim.setParameterSpecifiations(null);
		assertEquals(0,cim.getParameterSpecifications().length);
	}
	
	@Test
	public void testSetNonNullParamSpec() {
		ParameterBasedInjectionMetadata cim =
			new MutableConstructorInjectionMetadata(null);
		ParameterSpecification[] ps = new ParameterSpecification[2];
		ps[0] = new ImmutableIndexedParameterSpecification(0,new ReferenceValueObject("foo"));
		ps[1] = new ImmutableIndexedParameterSpecification(1,new ReferenceValueObject("bar"));
		cim.setParameterSpecifiations(ps);
		assertSame(ps,cim.getParameterSpecifications());		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testSetNonNullPSWithNullValue() {
		ParameterBasedInjectionMetadata cim =
			new MutableConstructorInjectionMetadata(null);
		ParameterSpecification[] ps = new ParameterSpecification[2];
		ps[0] = new ImmutableIndexedParameterSpecification(0,new ReferenceValueObject("foo"));
		cim.setParameterSpecifiations(ps);		
	}
}
