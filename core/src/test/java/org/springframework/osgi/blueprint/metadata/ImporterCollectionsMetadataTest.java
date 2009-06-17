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

import java.util.Collection;
import java.util.Iterator;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.RefListMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.Target;

/**
 * @author Costin Leau
 */
public class ImporterCollectionsMetadataTest extends BaseMetadataTest {

	@Override
	protected String getConfig() {
		return "/org/springframework/osgi/blueprint/config/importer-collections-elements.xml";
	}

	private ServiceReferenceMetadata getReferenceMetadata(String name) {
		ComponentMetadata metadata = blueprintContainer.getComponentMetadata(name);
		assertTrue(metadata instanceof ServiceReferenceMetadata);
		ServiceReferenceMetadata referenceMetadata = (ServiceReferenceMetadata) metadata;
		assertEquals("the registered name doesn't match the component name", name, referenceMetadata.getId());
		return referenceMetadata;
	}

	public void testSimpleList() throws Exception {
		RefListMetadata metadata = (RefListMetadata) getReferenceMetadata("simpleList");
	}

	public void testListeners() throws Exception {
		RefListMetadata metadata = (RefListMetadata) getReferenceMetadata("listeners");
		Collection<ReferenceListener> listeners = metadata.getReferenceListeners();
		assertEquals(3, listeners.size());

		Iterator<ReferenceListener> iterator = listeners.iterator();
		ReferenceListener listener = iterator.next();
		assertEquals("bindM", listener.getBindMethod());
		assertEquals("unbindM", listener.getUnbindMethod());
		assertTrue(listener.getListenerComponent() instanceof RefMetadata);
		listener = iterator.next();
		assertTrue(listener.getListenerComponent() instanceof Target);
		listener = iterator.next();
		assertTrue(listener.getListenerComponent() instanceof RefMetadata);
	}
}
