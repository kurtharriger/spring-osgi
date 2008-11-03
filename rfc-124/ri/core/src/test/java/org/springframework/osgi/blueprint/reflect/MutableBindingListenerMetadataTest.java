package org.springframework.osgi.blueprint.reflect;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class MutableBindingListenerMetadataTest {

	MutableBindingListenerMetadata b;
	
	@Before
	public void init() {
		this.b = new MutableBindingListenerMetadata(new ReferenceValueObject("foo"),"bind","unbind");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullValue() throws Exception {
		new MutableBindingListenerMetadata(null,"","");
	}
	
	@Test
	public void testBindMethod() throws Exception {
		assertEquals("bind",b.getBindMethodName());
		b.setBindMethod("foo");
		assertEquals("foo",b.getBindMethodName());
	}
	
	@Test
	public void testUnbindMethod() throws Exception {
		assertEquals("unbind",b.getUnbindMethodName());
		b.setUnbindMethod("foo");
		assertEquals("foo",b.getUnbindMethodName());
	}
	
	@Test
	public void testGetListenerComponent() throws Exception {
		ReferenceValueObject v = (ReferenceValueObject) b.getListenerComponent();
		assertEquals("foo",v.getComponentName());
	}
}
