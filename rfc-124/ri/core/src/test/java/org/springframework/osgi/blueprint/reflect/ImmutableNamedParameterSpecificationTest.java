package org.springframework.osgi.blueprint.reflect;

import org.junit.Test;
import static org.junit.Assert.*;

public class ImmutableNamedParameterSpecificationTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullName() {
		ReferenceValueObject rv = new ReferenceValueObject("foo");
		new ImmutableNamedParameterSpecification(null,rv);
	}
	
	@Test
	public void testNonNullName() {
		ReferenceValueObject rv = new ReferenceValueObject("foo");
		ImmutableNamedParameterSpecification nps = new ImmutableNamedParameterSpecification("foo",rv);
		assertEquals("foo",nps.getName());
	}
	
}
