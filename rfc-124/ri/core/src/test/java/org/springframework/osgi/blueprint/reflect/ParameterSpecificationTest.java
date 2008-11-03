package org.springframework.osgi.blueprint.reflect;

import org.junit.Test;
import org.osgi.service.blueprint.reflect.Value;
import static org.junit.Assert.*;

public class ParameterSpecificationTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullValue() {
		new PS(null);
	}
	
	@Test
	public void testNonNullValue() {
		ReferenceValueObject rv = new ReferenceValueObject("foo");
		PS p = new PS(rv);
		assertSame(rv,p.getValue());
	}
	
	// to help testing of abstract class
	private static class PS extends AbstractParameterSpecification {
		public PS(Value v) {
			super(v);
		}
	}
	
}
