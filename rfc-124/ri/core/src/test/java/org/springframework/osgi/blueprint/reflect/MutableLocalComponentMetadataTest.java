package org.springframework.osgi.blueprint.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.blueprint.reflect.FieldInjectionMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;

public class MutableLocalComponentMetadataTest {

	MutableLocalComponentMetadata lcm;
	
	@Before
	public void init() {
		this.lcm = new MutableLocalComponentMetadata("myComponent");
	}
	
	@Test
	public void testClassName() {
		assertNull(lcm.getClassName());
		lcm.setClassName("foo");
		assertEquals("foo",lcm.getClassName());
	}
	
	@Test
	public void testConstructorInjectionMetadata() {
		assertEquals(0, lcm.getConstructorInjectionMetadata().getParameterSpecifications().length);
		MutableConstructorInjectionMetadata cim = new MutableConstructorInjectionMetadata(null);
		lcm.setConstructorMetadata(cim);
		assertSame(cim,lcm.getConstructorInjectionMetadata());
	}
	
	@Test
	public void testDestroyMethod() {
		assertNull(lcm.getDestroyMethodName());
		lcm.setDestroyMethodName("foo");
		assertEquals("foo", lcm.getDestroyMethodName());
	}
	
	@Test
	public void testInitMethod() {
		assertNull(lcm.getInitMethodName());
		lcm.setInitMethodName("foo");
		assertEquals("foo", lcm.getInitMethodName());
	}
	
	@Test
	public void testMethodInjection() throws Exception {
		assertEquals(0,lcm.getMethodInjectionMetadata().length);
		MethodInjectionMetadata mim = new MutableMethodInjectionMetadata("fo,",null);
		MethodInjectionMetadata[] mims = new MethodInjectionMetadata[] {mim}; 
		lcm.setMethodInjectionMetadata(mims);
		assertSame(mims,lcm.getMethodInjectionMetadata());
	}
	
	@Test
	public void testFactoryComponent() throws Exception {
		assertNull(lcm.getFactoryComponent());
		MutableLocalComponentMetadata mcm = new MutableLocalComponentMetadata("foo");
		lcm.setFactoryComponent(mcm);
		assertSame(mcm, lcm.getFactoryComponent());
	}
	
	@Test
	public void testFactoryMethod() throws Exception {
		assertNull(lcm.getFactoryMethodMetadata());
		MethodInjectionMetadata mim = new MutableMethodInjectionMetadata("fo,",null);
		lcm.setFactoryMethodMetadata(mim);
		assertSame(mim,lcm.getFactoryMethodMetadata());
	}
	
	@Test
	public void testFieldInjection() throws Exception {
		assertEquals(0,lcm.getFieldInjectionMetadata().length);
		FieldInjectionMetadata[] fims = new FieldInjectionMetadata[0];
		lcm.setFieldInjectionMetadata(fims);
		assertSame(fims,lcm.getFieldInjectionMetadata());
	}
	
	@Test
	public void testParent() throws Exception {
		assertNull(lcm.getParent());
		MutableLocalComponentMetadata cm = new MutableLocalComponentMetadata("foo");
		lcm.setParentComponent(cm);
		assertSame(cm,lcm.getParent());
	}
	
	@Test
	public void testPropertyInjection() throws Exception {
		assertEquals(0,lcm.getPropertyInjectionMetadata().length);
		PropertyInjectionMetadata[] pims = new PropertyInjectionMetadata[0];
		lcm.setPropertyInjectionMetadata(pims);
		assertSame(pims,lcm.getPropertyInjectionMetadata());
	}
	
	@Test
	public void testScope() throws Exception {
		assertEquals(LocalComponentMetadata.SCOPE_SINGLETON, lcm.getScope());
		lcm.setScope("foo");
		assertEquals("foo",lcm.getScope());
	}
	
	@Test
	public void testIsAbstract() throws Exception {
		assertFalse(lcm.isAbstract());
		lcm.setAbstract(true);
		assertTrue(lcm.isAbstract());
	}
	
	@Test
	public void testLazy() throws Exception {
		assertFalse(lcm.isLazy());
		lcm.setLazy(true);
		assertTrue(lcm.isLazy());
	}
	
}
