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

package org.springframework.osgi.mock;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Simple enumeration mock backed by an array of objects.
 * 
 */
public class ArrayEnumerator<E> implements Enumeration<E> {

	private final E[] source;

	private int index = 0;


	public ArrayEnumerator(E[] source) {
		this.source = source;
	}

	public boolean hasMoreElements() {
		return source.length > index;
	}

	public E nextElement() {
		if (hasMoreElements())
			return (source[index++]);
		else
			throw new NoSuchElementException();
	}
}
