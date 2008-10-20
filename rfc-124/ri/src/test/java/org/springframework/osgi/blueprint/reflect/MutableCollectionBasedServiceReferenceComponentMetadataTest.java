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

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.blueprint.reflect.CollectionBasedServiceReferenceComponentMetadata;

public class MutableCollectionBasedServiceReferenceComponentMetadataTest {

	MutableCollectionBasedServiceReferenceComponentMetadata sr;
	
	@Before
	public void init() {
		sr = new MutableCollectionBasedServiceReferenceComponentMetadata("foo",
				MutableCollectionBasedServiceReferenceComponentMetadata.CollectionType.SET);
	}
	
	
	@Test
	public void testCollectionType() throws Exception {
		assertSame(Set.class,sr.getCollectionType());
	}
	
	@Test
	public void testComparator() throws Exception {
		assertNull(sr.getComparator());
		ReferenceValueObject v = new ReferenceValueObject("foo");
		sr.setComparator(v);
		assertSame(v,sr.getComparator());
	}
	
	@Test
	public void testNaturalOrdering() throws Exception {
		assertTrue(sr.isNaturalOrderingBasedComparison());
		sr.setNaturalOrderingBasedComparison(false);
		assertFalse(sr.isNaturalOrderingBasedComparison());
	}
	
	@Test
	public void testOrderingBasis() throws Exception {
		assertEquals(CollectionBasedServiceReferenceComponentMetadata.ORDER_BASIS_SERVICE_REFERENCES, 
				sr.getNaturalOrderingComparisonBasis());
		sr.setNaturalOrderingComparisonBasis(CollectionBasedServiceReferenceComponentMetadata.ORDER_BASIS_SERVICES);
		assertEquals(CollectionBasedServiceReferenceComponentMetadata.ORDER_BASIS_SERVICES,
				sr.getNaturalOrderingComparisonBasis());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBadOrderingBasis() throws Exception {
		sr.setNaturalOrderingComparisonBasis(5);
	}
}
