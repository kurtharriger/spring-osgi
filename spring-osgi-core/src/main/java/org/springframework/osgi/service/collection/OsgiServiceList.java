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

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.osgi.framework.BundleContext;

/**
 * OSGi service dynamic collection - allows iterating while the underlying
 * storage is being shrunk/expanded. This collection is read-only - its content
 * is being retrieved dynamically from the OSGi platform.
 * 
 * <strong>Note</strong>:It is <strong>not</strong> synchronized.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceList extends OsgiServiceCollection implements List {

	private final List storage = (List) serviceIDs;

	public OsgiServiceList(String clazz, String filter, BundleContext context, int contextClassLoader) {
		super(clazz, filter, context, contextClassLoader);
	}

	protected Collection createInternalDynamicStorage() {
		return new DynamicList();
	}

	public Object get(int index) {
		// get the id from the specified position and retrieve its associated
		// service
		return serviceReferences.get(storage.get(index));
	}

	public int indexOf(Object o) {
		// FIXME: implement this
		throw new UnsupportedOperationException();
	}

	public int lastIndexOf(Object o) {
		// FIXME: implement this
		throw new UnsupportedOperationException();
	}

	public ListIterator listIterator() {
		return listIterator(0);
	}

	public ListIterator listIterator(final int index) {
		return new ListIterator() {

			// dynamic iterator
			private final ListIterator iter = storage.listIterator(index);
			
			public void add(Object o) {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return iter.hasNext();
			}

			public boolean hasPrevious() {
				return iter.hasPrevious();
			}

			public Object next() {
				return serviceReferences.get(iter.next());
			}

			public int nextIndex() {
				return iter.nextIndex();
			}

			public Object previous() {
				return serviceReferences.get(iter.previous());
			}

			public int previousIndex() {
				return iter.previousIndex();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public void set(Object o) {
				throw new UnsupportedOperationException();
			}
		};
	}

	public List subList(int fromIndex, int toIndex) {
		// FIXME: implement this
		// note: the trick here is to return a list which is backed up by this
		// one (i.e. read-only)
		throw new UnsupportedOperationException();
	}

	// WRITE operations forbidden

	public Object remove(int index) {
		throw new UnsupportedOperationException();
	}

	public Object set(int index, Object o) {
		throw new UnsupportedOperationException();
	}

	public void add(int index, Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(int index, Collection c) {
		throw new UnsupportedOperationException();
	}

}
