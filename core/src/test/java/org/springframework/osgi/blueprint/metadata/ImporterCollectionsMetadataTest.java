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

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.RefCollectionMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
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
		RefCollectionMetadata metadata = (RefCollectionMetadata) getReferenceMetadata("simpleList");
		assertEquals(List.class, metadata.getCollectionType());
		Target comparator = metadata.getComparator();
		assertNotNull(comparator);
		assertTrue(comparator instanceof RefMetadata);
		assertEquals("comparator", ((RefMetadata) comparator).getComponentId());
	}

	public void testNestedComparator() throws Exception {
		RefCollectionMetadata metadata = (RefCollectionMetadata) getReferenceMetadata("nestedComparator");
		assertEquals(List.class, metadata.getCollectionType());
		Target comparator = metadata.getComparator();
		assertNotNull(comparator);
		assertTrue(comparator instanceof BeanMetadata);
	}

	public void testNestedRefComparator() throws Exception {
		RefCollectionMetadata metadata = (RefCollectionMetadata) getReferenceMetadata("nestedRefComparator");
		assertEquals(SortedSet.class, metadata.getCollectionType());
		Target comparator = metadata.getComparator();
		assertNotNull(comparator);
		assertTrue(comparator instanceof RefMetadata);
		assertEquals("compa", ((RefMetadata) comparator).getComponentId());
	}

	public void testMemberType() throws Exception {
		RefCollectionMetadata metadata = (RefCollectionMetadata) getReferenceMetadata("memberType");
		assertEquals(Set.class, metadata.getCollectionType());
	}

	public void testSortedSet() throws Exception {
		RefCollectionMetadata metadata = (RefCollectionMetadata) getReferenceMetadata("sortedSet");
		assertEquals(SortedSet.class, metadata.getCollectionType());
	}
}
