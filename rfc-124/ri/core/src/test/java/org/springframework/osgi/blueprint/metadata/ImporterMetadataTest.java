/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.blueprint.metadata;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;

/**
 * @author Costin Leau
 */

public class ImporterMetadataTest extends BaseMetadataTest {

	@Override
	protected String getConfig() {
		return "/org/springframework/osgi/blueprint/config/importer-elements.xml";
	}

	private ServiceReferenceComponentMetadata getReferenceMetadata(String name) {
		ComponentMetadata metadata = moduleContext.getComponentMetadata(name);
		assertTrue(metadata instanceof ServiceReferenceComponentMetadata);
		ServiceReferenceComponentMetadata referenceMetadata = (ServiceReferenceComponentMetadata) metadata;
		assertEquals("the registered name doesn't match the component name", name, referenceMetadata.getName());
		return referenceMetadata;
	}

	public void testSimpleBean() throws Exception {
		ServiceReferenceComponentMetadata metadata = getReferenceMetadata("simple");
		assertNull(metadata.getFilter());
		Set<String> intfs = metadata.getInterfaceNames();
		assertEquals(Cloneable.class.getName(), intfs.iterator().next());
		assertEquals(ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY,
			metadata.getServiceAvailabilitySpecification());

	}

	public void testBeanWithOptions() throws Exception {
		ServiceReferenceComponentMetadata metadata = getReferenceMetadata("options");
		assertEquals("(name=foo)", metadata.getFilter());
		Set<String> intfs = metadata.getInterfaceNames();
		assertEquals(Serializable.class.getName(), intfs.iterator().next());
		assertEquals(ServiceReferenceComponentMetadata.OPTIONAL_AVAILABILITY,
			metadata.getServiceAvailabilitySpecification());
		System.out.println(metadata.getBindingListeners());

	}

	public void testMultipleInterfaces() throws Exception {
		ServiceReferenceComponentMetadata metadata = getReferenceMetadata("multipleInterfaces");
		Set<String> intfs = metadata.getInterfaceNames();
		System.out.println(intfs);
		Iterator<String> iter = intfs.iterator();
		assertEquals(Cloneable.class.getName(), iter.next());
		assertEquals(Serializable.class.getName(), iter.next());
		assertEquals(ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY,
			metadata.getServiceAvailabilitySpecification());
		System.out.println(metadata.getBindingListeners());
	}
}
