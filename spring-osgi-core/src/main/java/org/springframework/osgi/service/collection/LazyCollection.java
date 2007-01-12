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
import java.util.Iterator;

/**
 * Simple decorating collection which offers lazy creation semantics.
 * 
 * It will postpone the creation of the internal/actual collection until the
 * first operation that requires the collection to be created is invoked.
 * 
 * @author Costin Leau
 * 
 */
public abstract class LazyCollection implements Collection {

	private boolean isCreated = false;

	private Collection internalCollection;

	protected boolean isCreated() {
		return isCreated;
	}

	protected Collection getCollection() {
		if (!isCreated) {
			isCreated = true;
			internalCollection = createCollection();
		}
		return internalCollection;
	}

	/**
	 * Create the actual collection.
	 * 
	 * @return
	 */
	public abstract Collection createCollection();

	public boolean add(Object arg0) {
		return getCollection().add(arg0);
	}

	public boolean addAll(Collection arg0) {
		return getCollection().addAll(arg0);
	}

	public void clear() {
		getCollection().clear();
	}

	public boolean contains(Object o) {
		return getCollection().contains(o);
	}

	public boolean containsAll(Collection arg0) {
		return getCollection().containsAll(arg0);
	}

	public boolean isEmpty() {
		return getCollection().isEmpty();
	}

	public Iterator iterator() {
		return getCollection().iterator();
	}

	public boolean remove(Object o) {
		return getCollection().remove(o);
	}

	public boolean removeAll(Collection arg0) {
		return getCollection().removeAll(arg0);
	}

	public boolean retainAll(Collection arg0) {
		return getCollection().retainAll(arg0);
	}

	public int size() {
		return getCollection().size();
	}

	public Object[] toArray() {
		return toArray(new Object[0]);
	}

	public Object[] toArray(Object[] arg0) {
		return getCollection().toArray(arg0);
	}

}
