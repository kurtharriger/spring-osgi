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
package org.springframework.osgi.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * @author Costin Leau
 */
public class CollectionTestComponent {

	private Object value;

	/**
	 * Simple injection with a single string argument.
	 * 
	 * @param componentId The component identifier used for test verification purposes.
	 */
	public CollectionTestComponent() {
	}

	public void setProperties(Properties value) {
		setPropertyValue("properties", value, Properties.class);
		if (value != null) {
			value.setProperty("$$$$$$ABC$$$$$$", "abc");
			value.remove("$$$$$$ABC$$$$$$");
		}
	}

	public void setDate(Date value) {
		setPropertyValue("date", value, Date.class);
	}

	public void setMap(Map value) {
		setPropertyValue("map", value, Map.class);
		// ensure this is a mutable version, so add a unique element and remove it
		if (value != null) {
			value.put("$$$$$$ABC$$$$$$", "abc");
			value.remove("$$$$$$ABC$$$$$$");
		}
	}

	public void setSet(Set value) {
		setPropertyValue("set", value, Set.class);
		// ensure this is a mutable version, so add a unique element and remove it
		if (value != null) {
			value.add("$$$$$$ABC$$$$$$");
			value.remove("$$$$$$ABC$$$$$$");
		}
	}

	public void setList(List value) {
		setPropertyValue("list", value, List.class);
		// ensure this is a mutable version, so add a unique element and remove it
		if (value != null) {
			value.add("$$$$$$ABC$$$$$$");
			value.remove("$$$$$$ABC$$$$$$");
		}
	}

	public void setCollection(Collection value) {
		setPropertyValue("collection", value, Collection.class);
	}

	public void setSortedSet(SortedSet value) {
		setPropertyValue("sortedSet", value, SortedSet.class);
	}

	public void setStack(Stack value) {
		setPropertyValue("stack", value, Stack.class);
	}

	public void setArrayList(ArrayList value) {
		setPropertyValue("arrayList", value, ArrayList.class);
	}

	public void setLinkedList(LinkedList value) {
		setPropertyValue("linkedList", value, LinkedList.class);
	}

	public void setVector(Vector value) {
		setPropertyValue("vector", value, Vector.class);
	}

	public void setHashSet(HashSet value) {
		setPropertyValue("hashSet", value, HashSet.class);
	}

	public void setLinkedHashSet(LinkedHashSet value) {
		setPropertyValue("linkedHashSet", value, LinkedHashSet.class);
	}

	public void setTreeSet(TreeSet value) {
		setPropertyValue("treeSet", value, TreeSet.class);
	}

	public void setHashMap(HashMap value) {
		setPropertyValue("hashMap", value, HashMap.class);
	}

	public void setHashtable(Hashtable value) {
		setPropertyValue("hashtable", value, Hashtable.class);
	}

	public void setTreeMap(TreeMap value) {
		setPropertyValue("treeMap", value, TreeMap.class);
	}

	public void setDictionary(Dictionary value) {
		setPropertyValue("dictionary", value, Dictionary.class);
	}

	public void setSortedMap(SortedMap value) {
		setPropertyValue("sortedMap", value, SortedMap.class);
	}

	public void setConcurrentMap(ConcurrentMap value) {
		setPropertyValue("concurrentMap", value, ConcurrentMap.class);
	}

	public void setConcurrentHashMap(ConcurrentHashMap value) {
		setPropertyValue("concurrentHashMap", value, ConcurrentHashMap.class);
	}

	public void setQueue(Queue value) {
		setPropertyValue("queue", value, LinkedList.class);
	}

	private void setPropertyValue(String name, Object value, Object type) {
		this.value = value;
	}

	public Object getPropertyValue() {
		return this.value;
	}

}