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

import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.Target;

/**
 * @author Costin Leau
 */

public class ImporterMetadataTest extends BaseMetadataTest {

	@Override
	protected String getConfig() {
		return "/org/springframework/osgi/blueprint/config/importer-elements.xml";
	}

	private ServiceReferenceMetadata getReferenceMetadata(String name) {
		ComponentMetadata metadata = blueprintContainer.getComponentMetadata(name);
		assertTrue(metadata instanceof ServiceReferenceMetadata);
		ServiceReferenceMetadata referenceMetadata = (ServiceReferenceMetadata) metadata;
		assertEquals("the registered name doesn't match the component name", name, referenceMetadata.getId());
		return referenceMetadata;
	}

	public void testSimpleBean() throws Exception {
		ServiceReferenceMetadata metadata = getReferenceMetadata("simple");
		System.out.println(metadata.getClass().getName());
		assertNull(metadata.getFilter());
		String intf = metadata.getInterface();
		assertEquals(Cloneable.class.getName(), intf);
		assertEquals(ReferenceMetadata.AVAILABILITY_MANDATORY, metadata.getAvailability());
		assertEquals(0, metadata.getReferenceListeners().size());
	}

	public void testBeanWithOptions() throws Exception {
		ServiceReferenceMetadata metadata = getReferenceMetadata("options");
		assertEquals("(name=foo)", metadata.getFilter());
		String intf = metadata.getInterface();
		assertEquals(Serializable.class.getName(), intf);
		assertEquals(ReferenceMetadata.AVAILABILITY_OPTIONAL, metadata.getAvailability());
		Collection<ReferenceListener> listeners = metadata.getReferenceListeners();
		assertEquals(1, listeners.size());
	}

	public void testMultipleInterfaces() throws Exception {
		ServiceReferenceMetadata metadata = getReferenceMetadata("multipleInterfaces");
		String intf = metadata.getInterface();
		assertEquals(Cloneable.class.getName(), intf);
		assertEquals(ReferenceMetadata.AVAILABILITY_MANDATORY, metadata.getAvailability());
		assertEquals(0, metadata.getReferenceListeners().size());
	}

	public void testMultipleListeners() throws Exception {
		ServiceReferenceMetadata metadata = getReferenceMetadata("multipleListeners");
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

	public void testTimeout() throws Exception {
		ServiceReferenceMetadata metadata = getReferenceMetadata("timeout");
		assertTrue(metadata instanceof ReferenceMetadata);
		assertEquals(1234, ((ReferenceMetadata) metadata).getTimeout());
	}

	public void testNestedMandatoryReference() throws Exception {
		BeanMetadata metadata = (BeanMetadata) blueprintContainer.getComponentMetadata("nestedReference");
		BeanProperty prop = (BeanProperty) metadata.getProperties().get(0);
		Metadata value = prop.getValue();
		assertTrue(value instanceof ReferenceMetadata);
		ReferenceMetadata ref = (ReferenceMetadata) value;
		assertEquals(1000, ref.getTimeout());
	}
	
	public void testNestedMandatoryCollectionReference() throws Exception {
		BeanMetadata metadata = (BeanMetadata) blueprintContainer.getComponentMetadata("nestedCollectionReference");
		BeanProperty prop = (BeanProperty) metadata.getProperties().get(0);
		Metadata value = prop.getValue();
		assertTrue(value instanceof ReferenceListMetadata);
		ReferenceListMetadata ref = (ReferenceListMetadata) value;
		assertEquals(ReferenceListMetadata.USE_SERVICE_REFERENCE, ref.getMemberType());
	}

}