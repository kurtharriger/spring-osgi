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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;

/**
 * @author Costin Leau
 */

public class ExporterMetadataTest extends BaseMetadataTest {

	@Override
	protected String getConfig() {
		return "/org/springframework/osgi/blueprint/config/exporter-elements.xml";
	}

	private ServiceExportComponentMetadata getReferenceMetadata(String name) {
		ComponentMetadata metadata = moduleContext.getComponentMetadata(name);
		assertTrue(metadata instanceof ServiceExportComponentMetadata);
		ServiceExportComponentMetadata referenceMetadata = (ServiceExportComponentMetadata) metadata;
		assertEquals("the registered name doesn't match the component name", name, referenceMetadata.getName());
		return referenceMetadata;
	}

	public void testSimpleBean() throws Exception {
		ServiceExportComponentMetadata metadata = getReferenceMetadata("simple");
		assertEquals(ServiceExportComponentMetadata.EXPORT_MODE_DISABLED, metadata.getAutoExportMode());
		Set<String> intfs = metadata.getInterfaceNames();
		assertEquals(1, intfs.size());
		assertEquals(Map.class.getName(), intfs.iterator().next());
		assertEquals(123, metadata.getRanking());
		assertTrue(metadata.getRegistrationListeners().isEmpty());

		assertTrue(metadata.getExportedComponent() instanceof ReferenceValue);
		Map props = metadata.getServiceProperties();
		assertEquals("lip", props.get("fat"));
	}

	public void testNestedBean() throws Exception {
		ServiceExportComponentMetadata metadata = getReferenceMetadata("nested");
		assertEquals(ServiceExportComponentMetadata.EXPORT_MODE_ALL, metadata.getAutoExportMode());

		Set<String> intfs = metadata.getInterfaceNames();
		assertEquals(2, intfs.size());
		Iterator<String> iterator = intfs.iterator();
		assertEquals(Map.class.getName(), iterator.next());
		assertEquals(Serializable.class.getName(), iterator.next());

		assertEquals(0, metadata.getRanking());

		Collection<RegistrationListenerMetadata> listeners = metadata.getRegistrationListeners();
		Iterator<RegistrationListenerMetadata> iter = listeners.iterator();
		RegistrationListenerMetadata listener = iter.next();

		assertEquals("up", listener.getRegistrationMethodName());
		assertEquals("down", listener.getUnregistrationMethodName());
		assertEquals("listener", ((ReferenceValue) listener.getListenerComponent()).getComponentName());

		listener = iter.next();
		assertEquals("up", listener.getRegistrationMethodName());
		assertEquals("down", listener.getUnregistrationMethodName());
		assertTrue(listener.getListenerComponent() instanceof ComponentValue);

		assertTrue(metadata.getExportedComponent() instanceof ComponentValue);
	}
}