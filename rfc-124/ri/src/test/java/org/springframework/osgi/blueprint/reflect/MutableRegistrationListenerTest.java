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

import static org.junit.Assert.*;

import org.junit.Test;
import org.osgi.service.blueprint.reflect.Value;

public class MutableRegistrationListenerTest {

	@Test
	public void testConstruction() throws Exception {
		Value v = new ReferenceValueObject("ref");
		MutableRegistrationListenerMetadata l = new MutableRegistrationListenerMetadata(v,"goo","bar");
		assertSame(v,l.getListenerComponent());
		assertEquals("goo",l.getRegistrationMethodName());
		assertEquals("bar",l.getUnregistrationMethodName());
	}
	
	@Test
	public void testRegMethod() throws Exception {
		Value v = new ReferenceValueObject("ref");
		MutableRegistrationListenerMetadata l = new MutableRegistrationListenerMetadata(v,"goo","bar");
		l.setRegistrationMethodName("r");
		assertEquals("r",l.getRegistrationMethodName());
	}
	
	@Test
	public void testUnregMethod() throws Exception {
		Value v = new ReferenceValueObject("ref");
		MutableRegistrationListenerMetadata l = new MutableRegistrationListenerMetadata(v,"goo","bar");
		l.setUnregistrationMethodName("u");
		assertEquals("u",l.getUnregistrationMethodName());		
	}
}
