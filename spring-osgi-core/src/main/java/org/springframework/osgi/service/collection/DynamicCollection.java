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
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Collection which can be increased or reduced at runtime while iterating. This
 * collection is <strong>not</strong> synchronized.
 * 
 * @author Costin Leau
 * 
 */
public class DynamicCollection extends AbstractCollection {

	protected class DynamicIterator implements Iterator {
		/**
		 * cursor pointing to the element that has to be returned by next()
		 * method
		 */
		protected int cursor = 0;

		protected boolean removalAllowed = false;

		public boolean hasNext() {
			return (cursor < storage.size());
		}

		public Object next() {
			removalAllowed = true;
			if (hasNext()) {
				return storage.get(cursor++);
			}

			throw new NoSuchElementException();
		}

		public void remove() {
			// make sure the cursor is valid
			if (removalAllowed) {
				removalAllowed = false;
				DynamicCollection.this.remove(cursor - 1);
			}
			else
				throw new IllegalStateException();
		}

	}

	/** actual collection storage * */
	protected final List storage;

	/** list of weak references to the actual iterators * */
	protected final List iterators;

	public DynamicCollection() {
		this(16);
	}

	public DynamicCollection(int size) {
		storage = new ArrayList(size);
		iterators = new ArrayList(4);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.AbstractCollection#iterator()
	 */
	public Iterator iterator() {
		Iterator iter = new DynamicIterator();
		// TODO: does this has to be synchronized also (when updating the iterators and removing GC'ed ones)?
		iterators.add(new WeakReference(iter));
		return iter;
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.AbstractCollection#size()
	 */
	public int size() {
		return storage.size();
	}

	public boolean add(Object o) {
		return storage.add(o);
	}

	public boolean addAll(Collection c) {
		return storage.addAll(c);
	}

	public boolean contains(Object o) {
		return storage.contains(o);
	}

	public boolean containsAll(Collection c) {
		return storage.containsAll(c);
	}

	public boolean isEmpty() {
		return storage.isEmpty();
	}

	public boolean remove(Object o) {
		int index = storage.indexOf(o);

		if (index == -1)
			return false;

		remove(storage.indexOf(o));
		return true;
	}

	// remove an object from the list using the given index
	// this is required for cases where the underlying storage (a list) might
	// contain
	// duplicates.
	protected Object remove(int index) {
		Object o = null;
		synchronized (storage) {
			o = storage.remove(index);
			// update iterators
			int i = 0;
			do {
				if (!iterators.isEmpty()) {
					WeakReference ref = (WeakReference) iterators.get(i);
					DynamicIterator iter = (DynamicIterator) ref.get();
					// clean iterators reference
					if (iter == null) {
						iterators.remove(i);
					}
					else {
						// back the cursor
						if (index < iter.cursor)
							iter.cursor--;
						i++;
					}
				}
			} while (i < iterators.size());
		}
		return o;
	}

	public Object[] toArray() {
		return storage.toArray();
	}

	public Object[] toArray(Object[] array) {
		return storage.toArray(array);
	}

	public String toString() {
		return storage.toString();
	}

}
