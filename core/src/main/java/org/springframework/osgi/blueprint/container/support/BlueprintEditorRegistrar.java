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
package org.springframework.osgi.blueprint.container.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomMapEditor;

/**
 * Registrar holding the specific Blueprint editors. This class is used by the Spring DM extender for all Blueprint
 * contexts.
 * 
 * @author Costin Leau
 */
public class BlueprintEditorRegistrar implements PropertyEditorRegistrar {

	public void registerCustomEditors(PropertyEditorRegistry registry) {
		// Date
		registry.registerCustomEditor(Date.class, new DateEditor());
		// Collection concrete types
		registry.registerCustomEditor(Stack.class, new CustomCollectionEditor(Stack.class));
		registry.registerCustomEditor(Vector.class, new CustomCollectionEditor(Vector.class));

		registry.registerCustomEditor(HashSet.class, new CustomCollectionEditor(HashSet.class));
		registry.registerCustomEditor(LinkedHashSet.class, new CustomCollectionEditor(LinkedHashSet.class));
		registry.registerCustomEditor(TreeSet.class, new CustomCollectionEditor(TreeSet.class));

		registry.registerCustomEditor(ArrayList.class, new CustomCollectionEditor(ArrayList.class));
		registry.registerCustomEditor(LinkedList.class, new CustomCollectionEditor(LinkedList.class));

		// Map concrete types
		registry.registerCustomEditor(HashMap.class, new CustomMapEditor(HashMap.class));
		registry.registerCustomEditor(LinkedHashMap.class, new CustomMapEditor(LinkedHashMap.class));
		registry.registerCustomEditor(Hashtable.class, new CustomMapEditor(Hashtable.class));
		registry.registerCustomEditor(TreeMap.class, new CustomMapEditor(TreeMap.class));
		// JDK 5 types
		registry.registerCustomEditor(ConcurrentMap.class, new CustomMapEditor(ConcurrentHashMap.class));
		registry.registerCustomEditor(ConcurrentHashMap.class, new CustomMapEditor(ConcurrentHashMap.class));
		registry.registerCustomEditor(Queue.class, new CustomCollectionEditor(LinkedList.class));

		// Legacy types
		registry.registerCustomEditor(Dictionary.class, new CustomMapEditor(Hashtable.class));
	}
}
