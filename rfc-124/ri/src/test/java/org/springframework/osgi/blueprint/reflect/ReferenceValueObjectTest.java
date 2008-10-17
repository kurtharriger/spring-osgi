package org.springframework.osgi.blueprint.reflect;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReferenceValueObjectTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullName() {
		new ReferenceValueObject(null);
	}
	
	@Test
	public void testNonNullName() {
		ReferenceValueObject r = new ReferenceValueObject("foo");
		assertEquals("foo",r.getComponentName());
	}
	
}
