package org.springframework.osgi.blueprint.reflect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SetValueObjectTest {

	private SetValueObject s;
	
	@Before
	public void init() {
		s = new SetValueObject();
		s.add("foo");
		s.add("bar");
	}
	
	@Test
	public void testAdd() {
		s.add("goo");
		assertEquals(3,s.size());
	}
	
	@Test
	public void testAddAll() {
		List<Object> l = new ArrayList<Object>();
		l.add("goo");
		s.addAll(l);
		assertEquals(3,s.size());
	}
	
	@Test
	public void testClear() {
		s.clear();
		assertTrue(s.isEmpty());
	}
	
	@Test
	public void testContains() {
		assertTrue(s.contains("foo"));
		assertFalse(s.contains("zebede"));
	}

	@Test public void testContainsAll() {
		List<Object> l = new ArrayList<Object>();
		l.add("foo");
		assertTrue(s.containsAll(l));
	}
	
	@Test
	public void testIsEmpty() {
		assertFalse(s.isEmpty());
		s.clear();
		assertTrue(s.isEmpty());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testIterator() {
		Iterator it = s.iterator();
		assertTrue(it.hasNext());
		assertEquals("foo",it.next());
	}
	
	@Test
	public void testRemove() {
		s.remove("bar");
		assertEquals(1,s.size());
	}
	
	@Test 
	public void testRemoveAll() {
		List<Object> l = new ArrayList<Object>();
		l.add("foo");
		s.removeAll(l);
		assertEquals(1,s.size());
	}
	
	@Test
	public void testRetainAll() {
		List<Object> l = new ArrayList<Object>();
		l.add("foo");
		s.retainAll(l);
		assertEquals(1,s.size());		
	}
	
	@Test
	public void testSize() {
		assertEquals(2,s.size());
	}
	
	@Test
	public void testToArray() {
		assertEquals(2,s.toArray().length);
		Object[] os = new Object[2];
		Object[] o = s.toArray(os);
		assertSame("same array",o,os);
	}
}
