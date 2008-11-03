package org.springframework.osgi.blueprint.reflect;

import org.junit.Test;
import static org.junit.Assert.*;

public class ImmutableTypedParameterSpecificationTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullTypeName() {
		ReferenceValueObject rv = new ReferenceValueObject("foo");
		new ImmutableTypedParameterSpecification(null,rv);
	}
	
	@Test
	public void testNonNullTypeName() {
		ReferenceValueObject rv = new ReferenceValueObject("foo");
		ImmutableTypedParameterSpecification ps = new ImmutableTypedParameterSpecification("foo",rv);
		assertEquals("foo",ps.getTypeName());
	}
	
}
