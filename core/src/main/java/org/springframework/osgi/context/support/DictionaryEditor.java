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
package org.springframework.osgi.context.support;

import java.beans.PropertyEditorSupport;
import java.util.Dictionary;
import java.util.Map;

import org.springframework.osgi.util.MapBasedDictionary;

/**
 * Editor for <code>java.util.Dictionary</code>. Translates a Map into a
 * Dictionary using a wrapper class.
 * 
 * @author Costin Leau
 * @see java.util.Dictionary
 * @see java.util.Map
 * @see MapBasedDictionary
 */
public class DictionaryEditor extends PropertyEditorSupport {

	private final boolean nullAsEmptyDictionary;

	public DictionaryEditor() {
		this(false);
	}

	public DictionaryEditor(boolean nullAsEmptyDictionary) {
		this.nullAsEmptyDictionary = nullAsEmptyDictionary;
	}

	/**
	 * This implementation returns <code>null</code> to indicate that there is
	 * no appropriate text representation.
	 */
	public String getAsText() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	/**
	 * Convert the given value to a Dictionary.
	 */
	public void setValue(Object value) {
		if (value == null && this.nullAsEmptyDictionary) {
			super.setValue(new MapBasedDictionary(0));
		}

		else if (value == null || value instanceof Dictionary) {
			super.setValue(value);
		}
		else {
			if (value instanceof Map)
				super.setValue(new MapBasedDictionary((Map) value));
			else {
				throw new IllegalArgumentException("Value cannot be converted to Dictionary: " + value);
			}

		}
	}
}
