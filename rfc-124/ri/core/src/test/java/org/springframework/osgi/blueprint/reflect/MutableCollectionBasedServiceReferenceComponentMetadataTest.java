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
