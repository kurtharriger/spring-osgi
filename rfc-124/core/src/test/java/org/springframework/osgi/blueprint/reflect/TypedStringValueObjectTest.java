package org.springframework.osgi.blueprint.reflect;

import static org.junit.Assert.*;

import org.junit.Test;

public class TypedStringValueObjectTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullValue() {
		new TypedStringValueObject(null,"type");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullType() {
		new TypedStringValueObject("value",null);
	}
	
	@Test
	public void testNonNullValueAndType() {
		TypedStringValueObject tsv = new TypedStringValueObject("value","type");
		assertEquals("value", tsv.getStringValue());
		assertEquals("type",tsv.getTypeName());
	}
	
}
