package org.springframework.osgi.blueprint.reflect;

import static org.junit.Assert.*;

import org.junit.Test;

public class MutableUnaryServiceReferenceComponentMetadataTest {

	@Test
	public void testTimeout() throws Exception {
		MutableUnaryServiceReferenceComponentMetadata sr = new MutableUnaryServiceReferenceComponentMetadata("foo");
		assertEquals(-1,sr.getTimeout());
		sr.setTimeout(500L);
		assertEquals(500L, sr.getTimeout());
	}
	
}
