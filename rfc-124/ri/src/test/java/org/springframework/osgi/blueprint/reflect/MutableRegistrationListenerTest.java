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
