package org.springframework.osgi.blueprint.reflect;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReferenceNameValueObjectTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullName() {
		new ReferenceNameValueObject(null);
	}
	
	public void testNonNullName() {
		ReferenceNameValueObject ref = new ReferenceNameValueObject("foo");
		assertEquals("foo",ref.getReferenceName());
	}
	
}
