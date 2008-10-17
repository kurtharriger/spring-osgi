package org.springframework.osgi.blueprint.reflect;

import org.junit.Test;
import static org.junit.Assert.*;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;


public class ComponentValueObjectTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullComponent( ) {
		new ComponentValueObject(null);
	}
	
	public void testNonNullComponent() {
		LocalComponentMetadata value = new MutableLocalComponentMetadata("foo");
		ComponentValueObject cv = new ComponentValueObject(value);
		assertEquals("should be value we supplied",value,cv.getComponentMetadata());
	}
}
