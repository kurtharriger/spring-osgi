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
import java.util.Set;
import java.util.SortedSet;

import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.CollectionBasedServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.UnaryServiceReferenceComponentMetadata;

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
		assertEquals(0, metadata.getBindingListeners().size());
	}

	public void testBeanWithOptions() throws Exception {
		ServiceReferenceComponentMetadata metadata = getReferenceMetadata("options");
		assertEquals("(name=foo)", metadata.getFilter());
		Set<String> intfs = metadata.getInterfaceNames();
		assertEquals(Serializable.class.getName(), intfs.iterator().next());
		assertEquals(ServiceReferenceComponentMetadata.OPTIONAL_AVAILABILITY,
			metadata.getServiceAvailabilitySpecification());
		Collection<BindingListenerMetadata> listeners = metadata.getBindingListeners();
		assertEquals(1, listeners.size());
	}

	public void testMultipleInterfaces() throws Exception {
		ServiceReferenceComponentMetadata metadata = getReferenceMetadata("multipleInterfaces");
		Set<String> intfs = metadata.getInterfaceNames();
		Iterator<String> iter = intfs.iterator();
		assertEquals(Cloneable.class.getName(), iter.next());
		assertEquals(Serializable.class.getName(), iter.next());
		assertEquals(ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY,
			metadata.getServiceAvailabilitySpecification());
		assertEquals(0, metadata.getBindingListeners().size());
	}

	public void testMultipleListeners() throws Exception {
		ServiceReferenceComponentMetadata metadata = getReferenceMetadata("multipleListeners");
		Collection<BindingListenerMetadata> listeners = metadata.getBindingListeners();
		assertEquals(3, listeners.size());

		Iterator<BindingListenerMetadata> iterator = listeners.iterator();
		BindingListenerMetadata listener = iterator.next();
		assertEquals("bindM", listener.getBindMethodName());
		assertEquals("unbindM", listener.getUnbindMethodName());
		assertTrue(listener.getListenerComponent() instanceof ReferenceValue);
		listener = iterator.next();
		assertTrue(listener.getListenerComponent() instanceof ComponentValue);
		listener = iterator.next();
		assertTrue(listener.getListenerComponent() instanceof ReferenceValue);
	}

	public void testTimeout() throws Exception {
		ServiceReferenceComponentMetadata metadata = getReferenceMetadata("timeout");
		assertTrue(metadata instanceof UnaryServiceReferenceComponentMetadata);
		assertEquals(1234, ((UnaryServiceReferenceComponentMetadata) metadata).getTimeout());
	}

	public void testSimpleList() throws Exception {
		CollectionBasedServiceReferenceComponentMetadata metadata = (CollectionBasedServiceReferenceComponentMetadata) getReferenceMetadata("simpleList");
		assertEquals(List.class, metadata.getCollectionType());
		System.out.println(metadata.getComparator());
	}

	public void testNestedComparator() throws Exception {
		CollectionBasedServiceReferenceComponentMetadata metadata = (CollectionBasedServiceReferenceComponentMetadata) getReferenceMetadata("nestedComparator");
		assertEquals(List.class, metadata.getCollectionType());
		System.out.println(metadata.getComparator());
	}

	public void testNestedRefComparator() throws Exception {
	}

	public void testMemberType() throws Exception {
		CollectionBasedServiceReferenceComponentMetadata metadata = (CollectionBasedServiceReferenceComponentMetadata) getReferenceMetadata("memberType");
		assertEquals(Set.class, metadata.getCollectionType());
	}

	public void testSortedSet() throws Exception {
		CollectionBasedServiceReferenceComponentMetadata metadata = (CollectionBasedServiceReferenceComponentMetadata) getReferenceMetadata("sortedSet");
		System.out.println(metadata.getCollectionType());
		assertEquals(SortedSet.class, metadata.getCollectionType());
	}
}