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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ListValueObjectTest {
	
	ListValueObject l;

	@Before
	public void initTest() {
		l = new ListValueObject();
	}
	
	@Test
	public void testEmptyAtOnCreation() {
		assertTrue(l.isEmpty());
	}
	
	@Test
	public void testAdd() {
		l.add("foo");
		assertEquals(1,l.size());
		assertTrue(l.contains("foo"));
	}

	@Test
	public void testAddAll() {
		List<Object> toAdd = new ArrayList<Object>();
		toAdd.add("foo");
		toAdd.add("bar");
		l.addAll(toAdd);
		assertEquals(2,l.size());
		assertTrue(l.contains("foo"));
		assertTrue(l.contains("bar"));
	}
	
	@Test
	public void testClear() {
		l.add("foo");
		l.clear();
		assertTrue(l.isEmpty());
	}
	
	@Test
	public void testContains() {
		l.add("foo");
		assertTrue(l.contains("foo"));
	}
	
	@Test 
	public void testContainsAll() {
		List<Object> toAdd = new ArrayList<Object>();
		toAdd.add("foo");
		toAdd.add("bar");
		l.addAll(toAdd);
		assertTrue(l.containsAll(toAdd));
	}
	
	@Test
	public void testGet() {
		l.add("foo");
		assertEquals("foo",l.get(0));
	}
	
	@Test 
	public void testIndexOf() {
		l.add("foo");
		assertEquals(0,l.indexOf("foo"));
	}
	
	@Test
	public void testIsEmpty() {
		assertTrue(l.isEmpty());
		l.add("foo");
		assertFalse(l.isEmpty());
	}
	
	@Test 
	@SuppressWarnings("unchecked")
	public void testIterator() {
		l.add("foo");
		Iterator<Object> it = l.iterator();
		assertTrue(it.hasNext());
		assertEquals("foo",it.next());
		assertFalse(it.hasNext());
	}
	
	@Test
	public void testLastIndexOf() {
		assertEquals(-1,l.lastIndexOf("foo"));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testListIterator() {
		l.add("foo");
		ListIterator<Object> li = l.listIterator();
		assertTrue(li.hasNext());
		assertEquals("foo",li.next());
		assertFalse(li.hasNext());		
	}
	
	@Test
	public void testRemove() {
		l.add("foo");
		l.remove("foo");
		assertTrue(l.isEmpty());
	}
	
	@Test
	public void testRetainAll() {
		l.add("foo"); l.add("bar");
		List<Object> toRetain = new ArrayList<Object>();
		toRetain.add("foo");
		l.retainAll(toRetain);
		assertTrue(l.contains("foo"));
		assertFalse(l.contains("bar"));
	}
	
	@Test
	public void testSet() {
		l.add("foo");
		l.set(0, "bar");
		assertEquals("bar",l.get(0));
	}
	
	@Test
	public void testSubList() {
		l.add("foo");
		l.add("bar");
		l.subList(0, 1).clear();
		assertEquals(1,l.size());
	}
	
	@Test
	public void testToArray() {
		l.add("foo");
		assertEquals("foo",l.toArray()[0]);
	}
	
	@Test
	public void testSize() {
		assertEquals(0, l.size());
		l.add("foo");
		assertEquals(1, l.size());
	}
}
