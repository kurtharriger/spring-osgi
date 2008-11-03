package org.springframework.osgi.blueprint.reflect;

import org.junit.Test;
import static org.junit.Assert.*;

public class ImmutableIndexedParameterSpecificationTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNegativeIndex() {
		ReferenceValueObject v = new ReferenceValueObject("foo");
		new ImmutableIndexedParameterSpecification(-1,v);
	}
	
	@Test
	public void testNonNegativeIndex() {
		ReferenceValueObject v = new ReferenceValueObject("foo");
		ImmutableIndexedParameterSpecification ps = 
			new ImmutableIndexedParameterSpecification(1,v);
		assertEquals(1,ps.getIndex());
	}
	
}
