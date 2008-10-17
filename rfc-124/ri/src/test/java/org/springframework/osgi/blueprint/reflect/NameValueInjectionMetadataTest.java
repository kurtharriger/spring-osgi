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
