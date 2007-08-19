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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.ObjectUtils;

/**
 * Class utility used internally. Contains mianly class inheritance mechanisms
 * used when creating OSGi service proxies.
 * 
 * @author Costin Leau
 * 
 */
public abstract class ClassUtils {

	/**
	 * Include only the interfaces inherited from superclasses or implemented by
	 * the current class.
	 */
	public static final int INCLUDE_INTERFACES = 1;

	/**
	 * Include only the class hierarchy (interfaces are excluded).
	 */
	public static final int INCLUDE_CLASS_HIERARCHY = 2;

	/**
	 * Include all inherited classes (classes or interfaces).
	 */
	public static final int INCLUDE_ALL_CLASSES = INCLUDE_INTERFACES | INCLUDE_CLASS_HIERARCHY;

	/**
	 * Determining if multiple classes(not interfaces) are specified, without
	 * any relation to each other. Interfaces will simply be ignored.
	 * 
	 * @param classes an array of classes
	 * @return true if at least two classes unrelated to each other are found,
	 * false otherwise
	 */
	public static boolean containsUnrelatedClasses(Class[] classes) {
		if (ObjectUtils.isEmpty(classes))
			return false;

		Class clazz = null;
		// check if is more then one class specified
		for (int i = 0; i < classes.length; i++) {
			if (!classes[i].isInterface()) {
				if (clazz == null)
					clazz = classes[i];
				// check relationship
				else {
					if (clazz.isAssignableFrom(classes[i]))
						// clazz is a parent, switch with the child
						clazz = classes[i];
					else if (!classes[i].isAssignableFrom(clazz))
						return true;

				}
			}
		}

		// everything is in order
		return false;
	}

	/**
	 * Parse the given class array and eliminate parents of existing classes.
	 * 
	 * @param classes array of classes
	 * @return a new array without superclasses
	 */
	public static Class[] removeParents(Class[] classes) {
		if (ObjectUtils.isEmpty(classes))
			return new Class[0];

		List clazz = new ArrayList(classes.length);
		for (int i = 0; i < classes.length; i++) {
			clazz.add(classes[i]);
		}

		// remove null elements
		while (clazz.remove(null)) {
		}

		// only one class is allowed
		// there can be multiple interfaces
		// parents of classes inside the array are removed

		boolean dirty;
		do {
			dirty = false;
			for (int i = 0; i < clazz.size(); i++) {
				Class currentClass = (Class) clazz.get(i);
				for (int j = 0; j < clazz.size(); j++) {
					if (i != j) {
						if (currentClass.isAssignableFrom((Class) clazz.get(j))) {
							clazz.remove(i);
							i--;
							dirty = true;
							break;
						}
					}
				}
			}
		} while (dirty);

		return (Class[]) clazz.toArray(new Class[clazz.size()]);
	}

	/**
	 * Return an array of parent classes for the given class. The mode paramater
	 * indicates whether only interfaces should be included, classes or both.
	 * 
	 * @see #INCLUDE_ALL_CLASSES
	 * @see #INCLUDE_CLASS_HIERARCHY
	 * @see #INCLUDE_INTERFACES
	 * 
	 * @param clazz
	 * @return
	 */
	public static Class[] getClassHierarchy(Class clazz, int mode) {

		Class[] classes = null;

		if (clazz != null && isModeValid(mode)) {

			Set composingClasses = new LinkedHashSet();

			if (includesMode(mode, INCLUDE_INTERFACES))
				composingClasses.addAll(org.springframework.util.ClassUtils.getAllInterfacesForClassAsSet(clazz));

			if (includesMode(mode, INCLUDE_CLASS_HIERARCHY)) {
				Class clz = clazz;
				do {
					composingClasses.add(clz);
					clz = clz.getSuperclass();
				} while (clz != null && clz != Object.class);
			}

			classes = (Class[]) composingClasses.toArray(new Class[composingClasses.size()]);

		}
		return (classes == null ? new Class[0] : classes);
	}

	/**
	 * Test if testedMode includes an expected mode.
	 * 
	 * @param testedMode
	 * @param mode
	 * @return
	 */
	private static boolean includesMode(int testedMode, int mode) {
		return (testedMode & mode) == mode;
	}

	/**
	 * Test if a mode is valid.
	 * 
	 * @param mode
	 * @return
	 */
	private static boolean isModeValid(int mode) {
		return (mode >= INCLUDE_INTERFACES && mode <= INCLUDE_ALL_CLASSES);
	}

	public static String[] toStringArray(Class[] array) {
		if (ObjectUtils.isEmpty(array))
			return new String[0];

		String[] strings = new String[array.length];

		for (int i = 0; i < array.length; i++) {
			strings[i] = array[i].getName();
		}
		
		return strings;
	}
}
