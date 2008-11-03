package org.springframework.osgi.blueprint.reflect;

import static org.junit.Assert.*;

import org.junit.Test;
import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;

public class MutableServiceReferenceComponentMetadataTest {

	private MutableServiceReferenceComponentMetadata sr = 
		new MutableServiceReferenceComponentMetadata("foo") {};
	
	@Test
	public void testFilter() throws Exception {
		assertEquals("",this.sr.getFilter());
		sr.setFilter("filter");
		assertEquals("filter",sr.getFilter());
	}
	
	@Test
	public void testInterfaces() throws Exception {
		assertEquals(0,sr.getInterfaceNames().length);
		String[] ifs = new String[1];
		ifs[0] = "IFoo";
		sr.setInterfaceNames(ifs);
		assertSame(ifs,sr.getInterfaceNames());
	}
	
	@Test
	public void testNullInterfaces() throws Exception {
		sr.setInterfaceNames(null);
		assertEquals(0,sr.getInterfaceNames().length);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullInterface() throws Exception {
		String[] ifs = new String[1];
		sr.setInterfaceNames(ifs);
	}
	
	@Test
	public void testServiceAvailability() throws Exception {
		assertEquals(ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY,sr.getServiceAvailabilitySpecification());
		sr.setServiceAvailability(ServiceReferenceComponentMetadata.OPTIONAL_AVAILABILITY);
		assertEquals(ServiceReferenceComponentMetadata.OPTIONAL_AVAILABILITY,sr.getServiceAvailabilitySpecification());
	}
	
	@Test
	public void testListeners() throws Exception {
		assertEquals(0,sr.getBindingListeners().length);
		BindingListenerMetadata[] blm = new BindingListenerMetadata[0];
		sr.setBindlingListeners(blm);
		assertSame(blm,sr.getBindingListeners());
	}
	
	@Test
	public void testNullListeners() throws Exception {
		sr.setBindlingListeners(null);
		assertEquals(0,sr.getBindingListeners().length);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNullListener() throws Exception {
		BindingListenerMetadata[] blm = new BindingListenerMetadata[1];
		sr.setBindlingListeners(blm);		
	}
}
