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

import java.util.Arrays;
import java.util.Collection;

/**
 * Smart decorating collection which return default values when the internal
 * collection is not created. Use this class only if the creation of the
 * internal collection returns an empty collection. <p/>
 * 
 * <strong>NOTE</strong>: returning the iterator always initializes the lazy
 * collection as dynamic collections (which allow modifications while iterating)
 * have to be supported. For the classic case, where iterators take a snapshot
 * of the collection or where modifications are not allowed, an empty iterator
 * can be returned if the underlying collection is not created.
 * 
 * @author Costin Leau
 * 
 */
public abstract class SmartLazyCollection extends LazyCollection {

	public void clear() {
		// clear only if non-lazy
		if (isCreated())
			getCollection().clear();
	}

	public boolean contains(Object o) {
		// collection is empty when lazy so return false
		if (!isCreated())
			return false;
		return super.contains(o);
	}

	public boolean containsAll(Collection arg0) {
		if (!isCreated())
			return false;
		return super.containsAll(arg0);
	}

	public boolean isEmpty() {
		if (!isCreated())
			return true;
		return super.isEmpty();
	}

	public boolean remove(Object o) {
		if (!isCreated())
			return false;
		return super.remove(o);
	}

	public boolean removeAll(Collection arg0) {
		if (!isCreated())
			return false;
		return super.removeAll(arg0);
	}

	public int size() {
		if (!isCreated())
			return 0;
		return super.size();
	}

	public Object[] toArray(Object[] array) {
		if (!isCreated()) {
			Arrays.fill(array, null);
			return array;
		}

		return super.toArray(array);
	}

}
