/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.service.importer.support.internal.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.springframework.util.Assert;

/**
 * A specilized subtype of DynamicList which impose an order between its
 * elements.
 * 
 * @author Costin Leau
 * 
 */
public class DynamicSortedList<T> extends DynamicList<T> {

	private final Comparator<? super T> comparator;


	public DynamicSortedList() {
		this((Comparator<? super T>) null);
	}

	public DynamicSortedList(Comparator<? super T> c) {
		super();
		this.comparator = c;

	}

	public DynamicSortedList(Collection<? extends T> c) {
		this.comparator = null;
		addAll(c);
	}

	public DynamicSortedList(int size) {
		super(size);
		this.comparator = null;
	}

	// this is very similar but not identical from DynamicSortedSet
	// the main difference is that duplicates are accepted
	@SuppressWarnings("unchecked")
	public boolean add(T o) {
		Assert.notNull(o);

		if (comparator == null && !(o instanceof Comparable))
			throw new ClassCastException("given object does not implement " + Comparable.class.getName()
					+ " and no Comparator is set on the collection");

		int index = 0;

		synchronized (storage) {
			index = Collections.binarySearch(storage, o, comparator);
			// duplicate found; it's okay since it's a list
			boolean duplicate = (index >= 0);

			// however, make sure we add the element at the end of the
			// duplicates
			if (duplicate) {
				boolean stillEqual = true;
				while (index + 1 < storage.size() && stillEqual) {

					stillEqual = false;
					T next = storage.get(index + 1);

					if ((comparator != null ? comparator.compare(o, next) == 0
							: ((Comparable<T>) o).compareTo(next) == 0)) {
						stillEqual = true;
						index++;
					}
				}
			}

			// translate index
			else
				index = -index - 1;

			if (duplicate)
				super.add(index + 1, o);
			else
				super.add(index, o);
		}
		return true;
	}

	//
	// DISABLED OPERATIONS
	// 

	public void add(int index, T o) {
		throw new UnsupportedOperationException("This is a sorted list; it is illegal to specify the element position");
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException("This is a sorted list; it is illegal to specify the element position");
	}

	public T set(int index, T o) {
		throw new UnsupportedOperationException("This is a sorted list; it is illegal to specify the element position");
	}
}