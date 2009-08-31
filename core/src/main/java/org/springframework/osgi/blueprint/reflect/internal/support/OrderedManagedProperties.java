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
package org.springframework.osgi.blueprint.reflect.internal.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.support.ManagedProperties;

/**
 * Extension that adds ordering to {@link ManagedProperties} class intended for preserving declaration order.
 * 
 * @author Costin Leau
 */
public class OrderedManagedProperties extends ManagedProperties {

	private final Map<Object, Object> orderedStorage = new LinkedHashMap<Object, Object>();

	public void clear() {
		orderedStorage.clear();
	}

	public boolean containsKey(Object key) {
		return orderedStorage.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return orderedStorage.containsValue(value);
	}

	public Set<java.util.Map.Entry<Object, Object>> entrySet() {
		return orderedStorage.entrySet();
	}

	public boolean equals(Object o) {
		return orderedStorage.equals(o);
	}

	public Object get(Object key) {
		return orderedStorage.get(key);
	}

	public int hashCode() {
		return orderedStorage.hashCode();
	}

	public boolean isEmpty() {
		return orderedStorage.isEmpty();
	}

	public Set<Object> keySet() {
		return orderedStorage.keySet();
	}

	public Object put(Object key, Object value) {
		return orderedStorage.put(key, value);
	}

	public void putAll(Map<? extends Object, ? extends Object> t) {
		orderedStorage.putAll(t);
	}

	public Object remove(Object key) {
		return orderedStorage.remove(key);
	}

	public int size() {
		return orderedStorage.size();
	}

	public Collection<Object> values() {
		return orderedStorage.values();
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		String val = getProperty(key);
		return (val == null ? defaultValue : val);
	}

	@Override
	public String getProperty(String key) {
		Object val = orderedStorage.get(key);
		return (val instanceof String ? (String) val : null);
	}

	@Override
	public Enumeration<?> propertyNames() {
		return new ArrayEnumeration<String>(filter(orderedStorage.keySet(), String.class));
	}

	@Override
	public synchronized Object setProperty(String key, String value) {
		return orderedStorage.put(key, value);
	}

	@Override
	public synchronized boolean contains(Object value) {
		return orderedStorage.containsKey(value);
	}

	@Override
	public synchronized Enumeration<Object> elements() {
		return new ArrayEnumeration<Object>(filter(orderedStorage.values(), Object.class));
	}

	@Override
	public synchronized Enumeration<Object> keys() {
		return new ArrayEnumeration<Object>(filter(orderedStorage.keySet(), Object.class));
	}

	@Override
	public synchronized String toString() {
		return orderedStorage.toString();
	}

	public Object merge(Object parent) {
		if (!isMergeEnabled()) {
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof Properties)) {
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		Properties merged = new OrderedManagedProperties();
		merged.putAll((Properties) parent);
		merged.putAll(this);
		return merged;
	}

	private <T> T[] filter(Collection<?> collection, Class<T> type) {
		List<T> list = new ArrayList<T>();
		for (Object member : collection) {
			if (type.isInstance(member)) {
				list.add((T) member);
			}
		}
		return (T[]) list.toArray(new Object[list.size()]);
	}

	private static class ArrayEnumeration<E> implements Enumeration<E> {

		private final E[] array;
		private int counter = 0;

		ArrayEnumeration(E[] array) {
			this.array = array;
		}

		public boolean hasMoreElements() {
			return (counter < array.length);
		}

		public E nextElement() {
			if (hasMoreElements())
				return array[counter++];
			throw new NoSuchElementException();
		}
	}
}