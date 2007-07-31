/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReadWriteLock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Utility class for dealing with collections. Mainly offers specialized
 * synchronized wrappers for normal, non-synchronized collections.
 * 
 * @author Costin Leau
 * 
 */
public abstract class CollectionsUtils {

	public static interface Op {
		Object op();
	}

	public static class RWTemplate {

		private final ReadWriteLock rwl;

		private final Lock r;

		private final Lock w;

		RWTemplate(ReadWriteLock lock) {
			this.rwl = lock;
			this.r = rwl.readLock();
			this.w = rwl.writeLock();
		}

		RWTemplate() {
			this(new ReentrantReadWriteLock());
		}

		Object doRead(Op operation) {
			r.lock();
			try {
				return operation.op();
			}
			finally {
				r.unlock();
			}
		}

		Object doWrite(Op operation) {
			w.lock();
			try {
				return operation.op();
			}
			finally {
				w.unlock();
			}

		}
	}

	static class RWLockCollection implements Collection {
		final Collection col;

		final ReadWriteLock rwl = new ReentrantReadWriteLock();

		final Lock r = rwl.readLock();

		final Lock w = rwl.writeLock();

		public RWLockCollection(Collection c) {
			this.col = c;
		}

		public boolean add(Object o) {
			w.lock();
			try {
				return col.add(o);
			}
			finally {
				w.unlock();
			}
		}

		public boolean addAll(Collection c) {
			w.lock();
			try {
				return col.addAll(c);
			}
			finally {
				w.unlock();
			}

		}

		public void clear() {
			w.lock();
			try {
				col.clear();
			}
			finally {
				w.unlock();
			}
		}

		public boolean contains(Object o) {
			r.lock();
			try {
				return col.contains(o);
			}
			finally {
				r.unlock();
			}
		}

		public boolean containsAll(Collection c) {
			r.lock();
			try {
				return col.containsAll(c);
			}
			finally {
				r.unlock();
			}
		}

		public boolean isEmpty() {
			r.lock();
			try {
				return col.isEmpty();
			}
			finally {
				r.unlock();
			}
		}

		public Iterator iterator() {
			return col.iterator();
		}

		public boolean remove(Object o) {
			w.lock();
			try {
				return col.remove(o);
			}
			finally {
				w.unlock();
			}
		}

		public boolean removeAll(Collection c) {
			w.lock();
			try {
				return col.remove(c);
			}
			finally {
				w.unlock();
			}
		}

		public boolean retainAll(Collection c) {
			w.lock();
			try {
				return col.retainAll(c);
			}
			finally {
				w.unlock();
			}
		}

		public int size() {
			r.lock();
			try {
				return col.size();
			}
			finally {
				r.unlock();
			}
		}

		public Object[] toArray() {
			r.lock();
			try {
				return col.toArray();
			}
			finally {
				r.unlock();
			}
		}

		public Object[] toArray(Object[] a) {
			r.lock();
			try {
				return col.toArray(a);
			}
			finally {
				r.unlock();
			}
		}
	}

	static class RWLockList extends RWLockCollection implements List {

		public RWLockList(List l) {
			super(l);
		}

		public void add(int index, Object element) {
			w.lock();
			try {
				((List) col).add(index, element);
			}
			finally {
				w.unlock();
			}
		}

		public boolean addAll(int index, Collection c) {
			w.lock();
			try {
				return ((List) col).addAll(index, c);
			}
			finally {
				w.unlock();
			}

		}

		public Object get(int index) {
			r.lock();
			try {
				return ((List) col).get(index);
			}
			finally {
				r.unlock();
			}
		}

		public int indexOf(Object o) {
			r.lock();
			try {
				return ((List) col).indexOf(o);
			}
			finally {
				r.unlock();
			}

		}

		public int lastIndexOf(Object o) {
			r.lock();
			try {
				return ((List) col).lastIndexOf(o);
			}
			finally {
				r.unlock();
			}

		}

		public ListIterator listIterator() {
			return ((List) col).listIterator();
		}

		public ListIterator listIterator(int index) {
			return ((List) col).listIterator(index);
		}

		public Object remove(int index) {
			w.lock();
			try {
				return ((List) col).remove(index);
			}
			finally {
				w.unlock();
			}
		}

		public Object set(int index, Object element) {
			w.lock();
			try {
				return ((List) col).set(index, element);
			}
			finally {
				w.unlock();
			}
		}

		// TODO: not synchronized
		public List subList(int fromIndex, int toIndex) {
			return ((List) col).subList(fromIndex, toIndex);
		}

	}

	public static Collection readWriteLockCollection(Collection c) {
		return new RWLockCollection(c);
	}

	public static List readWriteLockList(List l) {
		return new RWLockList(l);
	}

}
