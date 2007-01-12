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
package org.springframework.osgi.context.support.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.springframework.osgi.service.collection.DynamicCollection;

public class DynamicCollectionTest extends TestCase {

	private Collection dynamicCollection;

	private Iterator iter;

	protected void setUp() throws Exception {
		dynamicCollection = new DynamicCollection();
		iter = dynamicCollection.iterator();
	}

	protected void tearDown() throws Exception {
		dynamicCollection = null;
		iter = null;
	}

	public void testAddWhileIterating() throws Exception {
		assertTrue(dynamicCollection.isEmpty());
		assertFalse(iter.hasNext());
		Object a = new Object();
		dynamicCollection.add(a);
		assertTrue(iter.hasNext());
		assertSame(a, iter.next());
		assertFalse(iter.hasNext());
	}

	public void testRemoveWhileIterating() throws Exception {
		assertTrue(dynamicCollection.isEmpty());
		assertFalse(iter.hasNext());
		Object a = new Object();
		Object b = new Object();
		Object c = new Object();
		dynamicCollection.add(a);
		dynamicCollection.add(b);
		dynamicCollection.add(c);

		assertSame(a, iter.next());
		dynamicCollection.remove(b);
		assertTrue(iter.hasNext());
		assertSame(c, iter.next());
	}

	public void testRemovePreviouslyIteratedWhileIterating() throws Exception {
		assertTrue(dynamicCollection.isEmpty());
		assertFalse(iter.hasNext());

		Object a = new Object();
		Object b = new Object();
		dynamicCollection.add(a);
		dynamicCollection.add(b);

		assertSame(a, iter.next());
		assertTrue(iter.hasNext());
		dynamicCollection.remove(a);
		// still have b
		assertTrue(iter.hasNext());
	}

	public void testRemoveUniteratedWhileIterating() throws Exception {
		assertTrue(dynamicCollection.isEmpty());
		assertFalse(iter.hasNext());

		Object a = new Object();
		Object b = new Object();
		Object c = new Object();
		dynamicCollection.add(a);
		dynamicCollection.add(b);
		dynamicCollection.add(c);

		assertSame(a, iter.next());
		assertTrue(iter.hasNext());
		dynamicCollection.remove(a);
		// still have b
		assertTrue(iter.hasNext());
		dynamicCollection.remove(b);
		// still have c
		assertTrue(iter.hasNext());
		assertSame(c, iter.next());
	}

	public void testIteratorRemove() throws Exception {
		Object a = new Object();
		Object b = new Object();
		Object c = new Object();

		dynamicCollection.add(a);
		dynamicCollection.add(b);
		dynamicCollection.add(c);

		assertTrue(iter.hasNext());
		try {
			iter.remove();
			fail("should have thrown exception");
		}
		catch (IllegalStateException ex) {
			// expected
		}

		assertSame(a, iter.next());
		assertSame(b, iter.next());
		// remove b
		iter.remove();

		assertEquals(2, dynamicCollection.size());
		assertSame(c, iter.next());
		// remove c
		iter.remove();
		assertEquals(1, dynamicCollection.size());

		try {
			iter.remove();
			fail("should have thrown exception");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	public void testRemoveAllWhileIterating() throws Exception {
		Object a = new Object();
		Object b = new Object();
		Object c = new Object();

		dynamicCollection.add(a);
		dynamicCollection.add(b);
		dynamicCollection.add(c);
		
		Collection col = new ArrayList();
		col.add(a);
		col.add(c);
		
		assertSame(a, iter.next());
		// remove a and c
		dynamicCollection.removeAll(col);
		assertSame(b, iter.next());
		assertFalse(iter.hasNext());
	}
	
	public void testAddAllWhileIterating() throws Exception {
		Object a = new Object();
		Object b = new Object();
		Object c = new Object();

		dynamicCollection.add(a);
		
		Collection col = new ArrayList();
		col.add(b);
		col.add(c);
		
		assertSame(a, iter.next());
		assertFalse(iter.hasNext());
		dynamicCollection.addAll(col);
		assertSame(b, iter.next());
		assertSame(c, iter.next());
	}
}
