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
import java.util.NoSuchElementException;
import java.util.SortedSet;

import org.springframework.util.Assert;

/**
 * Dynamic sorted set. The elements added at runtime, while preserve their
 * natural order which means
 * 
 * @author Costin Leau
 * 
 */
public class DynamicSortedSet<E> extends DynamicSet<E> implements SortedSet<E> {

	private final Comparator<? super E> comparator;


	public DynamicSortedSet() {
		this((Comparator<? super E>) null);
	}

	public DynamicSortedSet(Collection<? extends E> c) {
		comparator = null;
		addAll(c);
	}

	public DynamicSortedSet(int size) {
		super(size);
		comparator = null;
	}

	public DynamicSortedSet(SortedSet<E> ss) {
		this.comparator = ss.comparator();
		addAll(ss);
	}

	public DynamicSortedSet(Comparator<? super E> c) {
		this.comparator = c;

	}

	public Comparator<? super E> comparator() {
		return comparator;
	}

	public boolean add(E o) {
		Assert.notNull(o);

		if (comparator == null && !(o instanceof Comparable))
			throw new ClassCastException("given object does not implement " + Comparable.class.getName()
					+ " and no Comparator is set on the collection");

		int index = 0;

		synchronized (storage) {
			index = Collections.binarySearch(storage, o, comparator);
			// duplicate found; bail out
			if (index >= 0)
				return false;

			// translate index
			index = -index - 1;

			super.add(index, o);
		}

		return true;
	}

	public boolean remove(Object o) {
		Assert.notNull(o);
		return super.remove(o);
	}

	public E first() {
		synchronized (storage) {
			if (storage.isEmpty())
				throw new NoSuchElementException();
			else
				return storage.get(0);
		}
	}

	public SortedSet<E> headSet(Object toElement) {
		throw new UnsupportedOperationException();
	}

	public E last() {
		synchronized (storage) {
			if (storage.isEmpty())
				throw new NoSuchElementException();
			else
				return storage.get(storage.size() - 1);
		}
	}

	public SortedSet<E> subSet(Object fromElement, Object toElement) {
		throw new UnsupportedOperationException();
	}

	public SortedSet<E> tailSet(Object fromElement) {
		throw new UnsupportedOperationException();
	}
}