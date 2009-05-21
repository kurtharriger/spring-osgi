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
import java.util.List;
import java.util.Map;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.Target;

/**
 * @author Costin Leau
 */

public class ExporterMetadataTest extends BaseMetadataTest {

	@Override
	protected String getConfig() {
		return "/org/springframework/osgi/blueprint/config/exporter-elements.xml";
	}

	private ServiceMetadata getReferenceMetadata(String name) {
		ComponentMetadata metadata = blueprintContainer.getComponentMetadata(name);
		assertTrue(metadata instanceof ServiceMetadata);
		ServiceMetadata referenceMetadata = (ServiceMetadata) metadata;
		assertEquals("the registered name doesn't match the component name", name, referenceMetadata.getId());
		return referenceMetadata;
	}

	public void testSimpleBean() throws Exception {
		ServiceMetadata metadata = getReferenceMetadata("simple");
		assertEquals(ServiceMetadata.AUTO_EXPORT_DISABLED, metadata.getAutoExportMode());
		List<String> intfs = metadata.getInterfaceNames();
		assertEquals(1, intfs.size());
		assertEquals(Map.class.getName(), intfs.iterator().next());
		assertEquals(123, metadata.getRanking());
		assertTrue(metadata.getRegistrationListeners().isEmpty());

		assertTrue(metadata.getServiceComponent() instanceof RefMetadata);
		List<MapEntry> props = metadata.getServiceProperties();
		System.out.println(props);
		// assertEquals("lip", props.get("fat"));
	}

	public void testNestedBean() throws Exception {
		ServiceMetadata metadata = getReferenceMetadata("nested");
		assertEquals(ServiceMetadata.AUTO_EXPORT_ALL_CLASSES, metadata.getAutoExportMode());

		List<String> intfs = metadata.getInterfaceNames();
		assertEquals(2, intfs.size());
		Iterator<String> iterator = intfs.iterator();
		assertEquals(Map.class.getName(), iterator.next());
		assertEquals(Serializable.class.getName(), iterator.next());

		assertEquals(0, metadata.getRanking());

		Collection<RegistrationListener> listeners = metadata.getRegistrationListeners();
		Iterator<RegistrationListener> iter = listeners.iterator();
		RegistrationListener listener = iter.next();

		assertEquals("up", listener.getRegistrationMethodName());
		assertEquals("down", listener.getUnregistrationMethodName());
		assertEquals("listener", ((RefMetadata) listener.getListenerComponent()).getComponentId());

		listener = iter.next();
		assertEquals("up", listener.getRegistrationMethodName());
		assertEquals("down", listener.getUnregistrationMethodName());
		assertTrue(listener.getListenerComponent() instanceof Target);

		assertTrue(metadata.getServiceComponent() instanceof Target);
	}
}