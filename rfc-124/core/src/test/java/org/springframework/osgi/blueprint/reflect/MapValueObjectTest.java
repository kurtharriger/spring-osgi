package org.springframework.osgi.blueprint.reflect;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MapValueObjectTest {
	
	private MapValueObject m;
	
	@Before
	public void init() {
		m = new MapValueObject();
		m.put("foo", "bar");
	}
	
	@Test
	public void testClear() {
		m.clear();
		assertTrue(m.isEmpty());
	}
	
	@Test
	public void testContainsKey() {
		assertTrue(m.containsKey("foo"));
		assertFalse(m.containsKey("bar"));
	}
	
	@Test
	public void testContainsValue(){
		assertTrue(m.containsValue("bar"));
		assertFalse(m.containsValue("foo"));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testEntrySet() {
		Set<Map.Entry<Object,Object>> entries = m.entrySet();
		assertEquals(1,entries.size());
	}
	
	@Test
	public void testGet() {
		assertEquals("bar",m.get("foo"));
	}
	
	@Test
	public void testIsEmpty() {
		assertFalse(m.isEmpty());
		m.clear();
		assertTrue(m.isEmpty());
	}
	
	@Test
	public void testKeySet() {
		assertTrue(m.keySet().contains("foo"));
	}

	@Test
	public void testPut() {
		m.put("whizz","bang");
		assertEquals("bang",m.get("whizz"));
	}
	
	@Test
	public void testPutAll() {
		MapValueObject m2 = new MapValueObject();
		m2.putAll(m);
		assertEquals(1,m2.size());
	}
	
	@Test
	public void testRemove() {
		m.remove("foo");
		assertTrue(m.isEmpty());
	}
	
	@Test
	public void testSize() {
		assertEquals(1,m.size());
	}
	
	@Test
	public void testValues() {
		assertTrue(m.values().contains("bar"));
	}
	
}
