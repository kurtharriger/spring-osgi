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
		protected int cursor = 0;

		protected boolean removalAllowed = false;

		public int getCursor() {
			return cursor;
		}

		public boolean hasNext() {
			return (cursor < storage.size());
		}

		public Object next() {
			if (hasNext()) {
				removalAllowed = true;
				return storage.get(cursor++);
			}

			throw new NoSuchElementException();
		}

		public void remove() {
			// make sure the cursor is valid
			if (removalAllowed && (cursor > 0) && ((cursor - 1) < storage.size())) {
				removalAllowed = false;
				Object obj = storage.get(cursor - 1);

				// FIXME: this fails if the object is added twice to the
				// collection since the first occurance will be deleted no
				// matter if the index points to another one.
				DynamicCollection.this.remove(obj);
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
		synchronized (storage) {
			int pos = storage.indexOf(o);
			if (pos >= 0) {
				storage.remove(o);
				// update iterators
				int i = 0;
				do {
					if (!iterators.isEmpty()) {
						WeakReference ref = (WeakReference) iterators.get(i);
						DynamicIterator iter = (DynamicIterator) ref.get();
						// clean reference
						if (iter == null) {
							iterators.remove(i);
						}
						else {
							// back the cursor
							if (pos < iter.cursor)
								iter.cursor--;
							i++;
						}
					}
				} while (i < iterators.size());
				return true;
			}
		}
		return false;
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
