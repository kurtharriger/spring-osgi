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
package org.springframework.osgi.util;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * Dictionary implementation backed by a map instance. While the JDK provides a
 * Dictionary implementation through Hashtable, the class itself is always
 * synchronized and does not maintain the internal order.
 * 
 * <p/> This simple wrapper, accepts any type of Map as backing storage allowing
 * more options in chosing the approapriate implementation. By default, a
 * {@link java.util.LinkedHashMap} is used, if no Map is specified.
 * 
 * <p/> This implementation will enforce the Dictionary behavior over the map
 * when it comes to handling null values. As opposed to a Map, the Dictionary
 * always throws {@link NullPointerException} if a given argument is null.
 * 
 * @see java.util.Map
 * @see java.util.Dictionary
 * @author Costin Leau
 */
public class MapBasedDictionary extends Dictionary implements Map {

	private Map map;

	/**
	 * Enumeration wrapper around an Iterator.
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class IteratorBasedEnumeration implements Enumeration {

		private Iterator it;

		public IteratorBasedEnumeration(Iterator it) {
			Assert.notNull(it);
			this.it = it;
		}

		public IteratorBasedEnumeration(Collection col) {
			this(col.iterator());
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Enumeration#hasMoreElements()
		 */
		public boolean hasMoreElements() {
			return it.hasNext();
		}

		/*
		 * (non-Javadoc)
		 * @see java.util.Enumeration#nextElement()
		 */
		public Object nextElement() {
			return it.next();
		}

	}

	public MapBasedDictionary(Map map) {
		Assert.notNull(map);
		this.map = map;
	}

	/**
	 * Default constructor.
	 * 
	 */
	public MapBasedDictionary() {
		this.map = new LinkedHashMap();
	}

	public MapBasedDictionary(int initialCapacity) {
		this.map = new LinkedHashMap(initialCapacity);
	}

	/**
	 * Constructor for dealing with existing Dictionary. Will copy the content
	 * into the inner Map.
	 * 
	 * @param dictionary
	 */
	public MapBasedDictionary(Dictionary dictionary) {
		this(new LinkedHashMap(), dictionary);
	}

	public MapBasedDictionary(Map map, Dictionary dictionary) {
		this(map);
		putAll(dictionary);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		map.clear();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
		return map.entrySet();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(Object key) {
		if (key == null)
			throw new NullPointerException();
		return map.get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
		return map.keySet();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
		if (key == null || value == null)
			throw new NullPointerException();

		return map.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map t) {
		map.putAll(t);
	}

	public void putAll(Dictionary dictionary) {
		if (dictionary != null)
			// copy the dictionary
			for (Enumeration enm = dictionary.keys(); enm.hasMoreElements();) {
				Object key = enm.nextElement();
				map.put(key, dictionary.get(key));
			}
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object key) {
		if (key == null)
			throw new NullPointerException();

		return map.remove(key);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	public int size() {
		return map.size();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		return map.values();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Dictionary#elements()
	 */
	public Enumeration elements() {
		return new IteratorBasedEnumeration(map.values());
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Dictionary#keys()
	 */
	public Enumeration keys() {
		return new IteratorBasedEnumeration(map.keySet());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return map.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		// this should work nicely since the Dictionary implementations inside the JDK are Maps also
		return map.equals(obj);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return map.hashCode();
	}

}
