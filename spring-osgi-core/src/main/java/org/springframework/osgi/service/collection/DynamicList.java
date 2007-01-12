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

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.springframework.osgi.service.collection.DynamicCollection.DynamicIterator;

/**
 * Subclass offering a List view for a DynamicCollection. This allows not just
 * forward, but also backwards iteration through <code>ListIterator</list>.
 * 
 * <strong>Note</strong>: some list write operations are not allowed at the moment.
 * @author Costin Leau
 *
 */
public class DynamicList extends DynamicCollection implements List {

	/**
	 * List iterator. Piggybacks on the iterator returns by the superclass.
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class DynamicListIterator extends DynamicIterator implements ListIterator {

		private DynamicListIterator(int index) {
			super.cursor = index;
		}

		public void add(Object o) {
			DynamicList.this.add(cursor, o);
		}

		public boolean hasNext() {
			return super.hasNext();
		}

		public boolean hasPrevious() {
			return (cursor - 1 >= 0);
		}

		public Object next() {
			return super.next();
		}

		public int nextIndex() {
			return cursor;
		}

		public Object previous() {
			if (hasPrevious()) {
				removalAllowed = true;
				return storage.get(--cursor);
			}

			throw new NoSuchElementException();
		}

		public int previousIndex() {
			return (cursor - 1);
		}

		public void remove() {
			DynamicList.this.remove(cursor);
		}

		public void set(Object o) {
			storage.set(cursor, o);
		}

	}

	public void add(int index, Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(int index, Collection c) {
		throw new UnsupportedOperationException();
	}

	public Object get(int index) {
		return storage.get(index);
	}

	public int indexOf(Object o) {
		return storage.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return storage.lastIndexOf(o);
	}

	public ListIterator listIterator() {
		ListIterator iter = new DynamicListIterator(0);
		iterators.add(new WeakReference(iter));
		return iter;
	}

	public ListIterator listIterator(int index) {
		return new DynamicListIterator(index);
	}

	public Object remove(int index) {
		throw new UnsupportedOperationException();
	}

	public Object set(int index, Object o) {
		return storage.set(index, o);
	}

	public List subList(int fromIndex, int toIndex) {
		return storage.subList(fromIndex, toIndex);
	}

}
