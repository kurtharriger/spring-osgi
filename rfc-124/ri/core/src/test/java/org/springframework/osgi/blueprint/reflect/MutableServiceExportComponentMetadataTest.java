
package org.springframework.osgi.blueprint.reflect;

import static org.junit.Assert.*;

import java.util.Properties;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;

public class MutableServiceExportComponentMetadataTest {

	MutableServiceExportComponentMetadata sec;


	@Before
	public void init() {
		this.sec = new MutableServiceExportComponentMetadata("foo", new ReferenceValueObject("bar"),
			new String[] { "IFoo" });
	}

	@Test
	public void testExportMode() throws Exception {
		assertEquals(ServiceExportComponentMetadata.EXPORT_MODE_DISABLED, sec.getAutoExportMode());
		sec.setAutoExportMode(ServiceExportComponentMetadata.EXPORT_MODE_ALL);
		assertEquals(ServiceExportComponentMetadata.EXPORT_MODE_ALL, sec.getAutoExportMode());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadExportMode() {
		sec.setAutoExportMode(-1);
	}

	@Test
	public void testExportedComponent() throws Exception {
		assertTrue(sec.getExportedComponent() instanceof ReferenceValueObject);
		ReferenceValueObject rv = new ReferenceValueObject("bar");
		sec.setExportedComponent(rv);
		assertSame(rv, sec.getExportedComponent());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadExportedComponentType() {
		sec.setExportedComponent(new TypedStringValueObject("foo", "bar"));
	}

	@Test
	public void testInterfaceNames() throws Exception {
		Set<String> ifs = sec.getInterfaceNames();
		assertEquals(1, ifs.size());
		assertEquals("IFoo", ifs.iterator().next());
		String[] newIfs = new String[] { "a", "b" };
		sec.setInterfaceNames(newIfs);
		assertArrayEquals(newIfs, sec.getInterfaceNames().toArray());
	}

	@Test
	public void testRanking() throws Exception {
		assertEquals(0, sec.getRanking());
		sec.setRanking(9);
		assertEquals(9, sec.getRanking());
	}

	@Test
	public void testRegistrationListeners() throws Exception {
		assertEquals(0, sec.getRegistrationListeners().size());
		RegistrationListenerMetadata[] rls = new RegistrationListenerMetadata[0];
		sec.setRegistrationListenerMetadata(rls);
		assertSame(rls, sec.getRegistrationListeners());
	}

	@Test
	public void testServiceProperties() throws Exception {
		assertTrue(sec.getServiceProperties().isEmpty());
		Properties p = new Properties();
		sec.setServiceProperties(p);
		assertSame(p, sec.getServiceProperties());
	}
}