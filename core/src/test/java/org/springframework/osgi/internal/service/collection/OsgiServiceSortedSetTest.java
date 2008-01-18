/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.osgi.internal.service.collection;

import java.util.Iterator;

import org.springframework.osgi.service.importer.internal.collection.OsgiServiceCollection;
import org.springframework.osgi.service.importer.internal.collection.OsgiServiceSortedSet;

/**
 * @author Costin Leau
 * 
 */
public class OsgiServiceSortedSetTest extends AbstractOsgiCollectionTest {
	private OsgiServiceSortedSet col;

	private Iterator iter;

	protected void setUp() throws Exception {
		super.setUp();
		col = (OsgiServiceSortedSet) super.col;
		iter = col.iterator();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		col = null;
		iter = null;
	}

	OsgiServiceCollection createCollection() {
		return new OsgiServiceSortedSet(null, context, getClass().getClassLoader(), createProxyCreator(new Class[] {
				Wrapper.class, Comparable.class }));
	}

	public void testOrderingWhileAdding() {
		Wrapper date1 = new DateWrapper(1);
		Wrapper date2 = new DateWrapper(2);
		Wrapper date3 = new DateWrapper(3);

		addService(date2);
		assertEquals(1, col.size());

		addService(date2);
		assertEquals(1, col.size());
		assertEquals(date2.toString(), col.first().toString());

		addService(date1);
		assertEquals(2, col.size());
		assertEquals(date1.toString(), col.first().toString());

		addService(date3);

		assertEquals(3, col.size());
		assertEquals(date1.toString(), col.first().toString());
		assertEquals(date3.toString(), col.last().toString());
	}

	public void testOrderingWhileRemoving() {
		Wrapper date1 = new DateWrapper(1);
		Wrapper date2 = new DateWrapper(2);
		Wrapper date3 = new DateWrapper(3);

		addService(date1);
		addService(date2);
		addService(date3);

		removeService(date2);
		assertEquals(2, col.size());

		assertEquals(date1.toString(), col.first().toString());
		assertEquals(date3.toString(), col.last().toString());

		removeService(date1);

		assertEquals(1, col.size());
		assertEquals(date3.toString(), col.first().toString());
		assertEquals(date3.toString(), col.last().toString());

	}

	public void testOrderingWhileIterating() {
		Wrapper date1 = new DateWrapper(1);
		Wrapper date2 = new DateWrapper(2);
		Wrapper date3 = new DateWrapper(3);

		addService(date2);

		assertTrue(iter.hasNext());
		assertEquals(date2.toString(), iter.next().toString());

		addService(date1);
		assertFalse(iter.hasNext());

		addService(date3);
		assertTrue(iter.hasNext());
		assertEquals(date3.toString(), iter.next().toString());
	}

	public void testRemovalWhileIterating() {
		Wrapper date1 = new DateWrapper(1);
		Wrapper date2 = new DateWrapper(2);
		Wrapper date3 = new DateWrapper(3);

		addService(date2);
		addService(date3);
		addService(date1);
		addService(date2);
		addService(date1);

		assertEquals("collection should not accept duplicates", 3, col.size());

		// date1
		assertEquals(date1.toString(), iter.next().toString());

		removeService(date1);

		// date2
		assertEquals(date2.toString(), iter.next().toString());

		removeService(date2);
		// date 3
		assertEquals(date3.toString(), iter.next().toString());
	}

}
