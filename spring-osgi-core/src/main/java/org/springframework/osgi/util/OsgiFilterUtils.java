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

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class for OSGi filters.
 * 
 * <p/>
 * 
 * Allows filter creation(as well as modication) using multiple classnames.
 * 
 * @author Costin Leau
 * 
 */

// TODO: add generic capability to add other items beside classes (such as BEAN_NAME_PROPERTY_KEY)
public abstract class OsgiFilterUtils {

	private static final char FILTER_BEGIN = '(';

	private static final char FILTER_END = ')';

	private static final String FILTER_AND_CONSTRAINT = "(&";

	private static final String OBJECT_CLASS_GROUP = FILTER_BEGIN + Constants.OBJECTCLASS + "=";

	/**
	 * Add the given class as an 'and'(&amp;) {@link Constants.OBJECTCLASS}
	 * constraint to the given filter. At least one parameter must be valid
	 * (non-null).
	 * 
	 * @param clazz class name - can be null
	 * @param filter an existing, valid filter
	 * @return updated filter containg the {@link Constants.OBJECTCLASS}
	 * constraint
	 */
	public static String unifyFilter(String clazz, String filter) {
		return unifyFilter(new String[] { clazz }, filter);
	}

	/**
	 * Add the given class to the given filter. At least one parameter must be
	 * valid.
	 * 
	 * @see #unifyFilter(String, String)
	 * @param clazz
	 * @param filter
	 * @return
	 */
	public static String unifyFilter(Class clazz, String filter) {
		if (clazz != null)
			return unifyFilter(clazz.getName(), filter);
		return unifyFilter((String) null, filter);
	}

	/**
	 * Add the given classes to the given filter. At least one parameter must be
	 * valid.
	 * 
	 * @see #unifyFilter(String[], String)
	 * @param classes
	 * @param filter
	 * @return
	 */
	public static String unifyFilter(Class[] classes, String filter) {
		if (ObjectUtils.isEmpty(classes))
			return unifyFilter(new String[0], filter);

		String classNames[] = new String[classes.length];
		for (int i = 0; i < classNames.length; i++) {
			if (classes[i] != null)
				classNames[i] = classes[i].getName();
		}
		return unifyFilter(classNames, filter);
	}

	/**
	 * Add the given classese as an 'and'(&amp;) {@link Constants.OBJECTCLASS}
	 * constraint to the given filter. At least one parameter must be valid
	 * (non-null).
	 * 
	 * @param classes array of classes name - can be null/empty
	 * @param filter an existing, valid filter
	 * @return updated filter containg the {@link Constants.OBJECTCLASS}
	 * constraint
	 */
	public static String unifyFilter(String[] classes, String filter) {
		boolean filterHasText = StringUtils.hasText(filter);

		if (classes == null)
			classes = new String[0];

		// number of valid (not-null) classes
		int validClassNames = classes.length;

		for (int i = 0; i < classes.length; i++) {
			if (classes[i] == null)
				validClassNames--;
		}

		if (validClassNames == 0)
			// just return the filter
			if (filterHasText)
				return filter;
			else
				throw new IllegalArgumentException("at least one parameter has to be not-null");

		// do a simple filter check - starts with ( and ends with )
		if (filterHasText && !(filter.charAt(0) == FILTER_BEGIN && filter.charAt(filter.length() - 1) == FILTER_END)) {
			throw new IllegalArgumentException("invalid filter: " + filter);
		}

		// the classes will be added in a subfilter which does searching only
		// after
		// the objectClass
		// i.e.
		// (&(objectClass=java.lang.Object)(objectClass=java.lang.Cloneable))
		//
		// this subfilter will be added with a & constraint to the given filter
		// if
		// that one exists
		// i.e. (&(&(objectClass=MegaObject)(objectClass=SuperObject))(<given
		// filter>))

		StringBuffer buffer = new StringBuffer();

		// a. big & constraint
		// (&
		if (filterHasText)
			buffer.append(FILTER_AND_CONSTRAINT);

		boolean moreThenOneClass = validClassNames > 1;

		// b. create objectClass subfilter (only if we have more then one class
		// (&(&
		if (moreThenOneClass) {
			buffer.append(FILTER_AND_CONSTRAINT);
		}

		// parse the classes and add the classname under objectClass
		for (int i = 0; i < classes.length; i++) {
			if (classes[i] != null) {
				// (objectClass=
				buffer.append(OBJECT_CLASS_GROUP);
				// <actual value>
				buffer.append(classes[i]);
				// )
				buffer.append(FILTER_END);
			}
		}

		// c. close the classes subfilter
		// )
		if (moreThenOneClass) {
			buffer.append(FILTER_END);
		}

		// d. add the rest of the filter
		if (filterHasText) {
			buffer.append(filter);
			// e. close the big filter
			buffer.append(FILTER_END);
		}

		return buffer.toString();
	}

	/**
	 * Validate the given String as a OSGi filter.
	 * 
	 * @param filter the filter expression
	 * @return true if the filter is valid, false otherwise
	 */
	public static boolean isValidFilter(String filter) {
		try {
			createFilter(filter);
			return true;
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	/**
	 * Create an OSGi filter from the given String. Translates the
	 * {@link InvalidSyntaxException} checked exception into an unchecked
	 * {@link IllegalArgumentException}.
	 * 
	 * @param filter filter string representation
	 * @return OSGi filter
	 */
	public static Filter createFilter(String filter) {
		Assert.hasText(filter, "invalid filter");
		try {
			return FrameworkUtil.createFilter(filter);
		}
		catch (InvalidSyntaxException ise) {
			throw (RuntimeException) new IllegalArgumentException("invalid filter: " + ise.getFilter()).initCause(ise);
		}
	}
}