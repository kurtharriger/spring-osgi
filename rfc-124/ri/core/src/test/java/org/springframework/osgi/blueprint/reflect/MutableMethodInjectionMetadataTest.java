package org.springframework.osgi.blueprint.reflect;

import org.junit.Test;
import static org.junit.Assert.*;

public class MutableMethodInjectionMetadataTest {

	@Test(expected=IllegalArgumentException.class)
	public void testNullName() {
		new MutableMethodInjectionMetadata(null,null);
	}
	
	@Test
	public void testNonNullName() {
		MutableMethodInjectionMetadata mim = new MutableMethodInjectionMetadata("foo",null);
		assertEquals("foo",mim.getName());
		
	}
	
}
