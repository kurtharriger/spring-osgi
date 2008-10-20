/*
 * Copyright 2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.osgi.blueprint.reflect;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;

public class MutableServiceExportComponentMetadataTest {

	MutableServiceExportComponentMetadata sec;
	
	@Before
	public void init() {
		this.sec = new MutableServiceExportComponentMetadata(
				"foo",new ReferenceValueObject("bar"),new String[] {"IFoo"});
	}
	
	
	@Test
	public void testExportMode() throws Exception {
		assertEquals(ServiceExportComponentMetadata.EXPORT_MODE_DISABLED, sec.getAutoExportMode());
		sec.setAutoExportMode(ServiceExportComponentMetadata.EXPORT_MODE_ALL);
		assertEquals(ServiceExportComponentMetadata.EXPORT_MODE_ALL, sec.getAutoExportMode());		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBadExportMode() {
		sec.setAutoExportMode(-1);
	}
	
	@Test
	public void testExportedComponent() throws Exception {
		assertTrue(sec.getExportedComponent() instanceof ReferenceValueObject);
		ReferenceValueObject rv = new ReferenceValueObject("bar");
		sec.setExportedComponent(rv);
		assertSame(rv,sec.getExportedComponent());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBadExportedComponentType() {
		sec.setExportedComponent(new TypedStringValueObject("foo","bar"));
	}
	
	@Test
	public void testInterfaceNames() throws Exception {
		String[] ifs = sec.getInterfaceNames();
		assertEquals(1,ifs.length);
		assertEquals("IFoo",ifs[0]);
		String[] newIfs = new String[] {"a","b"};
		sec.setInterfaceNames(newIfs);
		assertSame(newIfs,sec.getInterfaceNames());
	}
	
	@Test
	public void testRanking() throws Exception {
		assertEquals(0,sec.getRanking());
		sec.setRanking(9);
		assertEquals(9,sec.getRanking());
	}
	
	@Test
	public void testRegistrationListeners() throws Exception {
		assertEquals(0,sec.getRegistrationListeners().length);
		RegistrationListenerMetadata[] rls = new RegistrationListenerMetadata[0];
		sec.setRegistrationListenerMetadata(rls);
		assertSame(rls,sec.getRegistrationListeners());
	}
	
	@Test
	public void testServiceProperties() throws Exception {
		assertTrue(sec.getServiceProperties().isEmpty());
		Properties p = new Properties();
		sec.setServiceProperties(p);
		assertSame(p,sec.getServiceProperties());
	}
	
}
