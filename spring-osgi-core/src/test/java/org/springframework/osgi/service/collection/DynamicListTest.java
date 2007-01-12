/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.osgi.service.collection;

import java.util.List;
import java.util.ListIterator;

import junit.framework.TestCase;

/**
 * @author Costin Leau
 * 
 */
public class DynamicListTest extends TestCase {

	private List dynamicList;

	private ListIterator iter;

	protected void setUp() throws Exception {
		dynamicList = new DynamicList();
		iter = dynamicList.listIterator();
	}

	protected void tearDown() throws Exception {
		dynamicList = null;
		iter = null;
	}

	public void testAddWhileListIterating() throws Exception {
		assertTrue(dynamicList.isEmpty());
		assertFalse(iter.hasNext());
		Object a = new Object();
		dynamicList.add(a);
		assertTrue(iter.hasNext());
		assertFalse(iter.hasPrevious());
		assertSame(a, iter.next());
		assertFalse(iter.hasNext());
		assertTrue(iter.hasPrevious());
		assertSame(a, iter.previous());
	}

	public void testRemoveWhileListIterating() throws Exception {
		assertTrue(dynamicList.isEmpty());
		assertFalse(iter.hasNext());
		Object a = new Object();
		Object b = new Object();
		Object c = new Object();
		dynamicList.add(a);
		dynamicList.add(b);
		dynamicList.add(c);

		assertSame(a, iter.next());
		// remove b
		dynamicList.remove(b);
		assertTrue(iter.hasNext());
		assertSame(c, iter.next());
		assertFalse(iter.hasNext());
		assertSame(c, iter.previous());
		// remove c
		dynamicList.remove(c);
		assertFalse(iter.hasNext());
		assertTrue(iter.hasPrevious());
		assertSame(a, iter.previous());
	}

	public void testRemovePreviouslyIteratedWhileIterating() throws Exception {
		assertTrue(dynamicList.isEmpty());
		assertFalse(iter.hasNext());

		Object a = new Object();
		Object b = new Object();
		dynamicList.add(a);
		dynamicList.add(b);

		assertSame(a, iter.next());
		assertTrue(iter.hasNext());
		dynamicList.remove(a);
		// still have b
		assertTrue(iter.hasNext());
		assertFalse(iter.hasPrevious());
	}

	public void testListIteratorIndexes() throws Exception {
		assertTrue(dynamicList.isEmpty());
		assertFalse(iter.hasNext());
		
		Object a = new Object();
		Object b = new Object();
		dynamicList.add(a);
		dynamicList.add(b);
		
		assertEquals(0, iter.nextIndex());
		assertEquals(-1, iter.previousIndex());
		assertSame(a, iter.next());
		assertEquals(1, iter.nextIndex());
		assertEquals(0, iter.previousIndex());
		dynamicList.remove(b);
		assertEquals(1, iter.nextIndex());
		assertEquals(0, iter.previousIndex());

		assertSame(a, iter.previous());
		assertEquals(0, iter.nextIndex());
		assertEquals(-1, iter.previousIndex());
		
		dynamicList.remove(a);
		assertEquals(0, iter.nextIndex());
		assertEquals(-1, iter.previousIndex());

	}
}
