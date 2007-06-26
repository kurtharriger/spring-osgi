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

import java.beans.PropertyEditor;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

public class DictionaryEditorTest extends TestCase {

	private PropertyEditor pe;

	protected void setUp() throws Exception {
		pe = new DictionaryEditor();
	}

	protected void tearDown() throws Exception {
		pe = null;
	}

	public void testNullIsEmptyDictionary() throws Exception {
		pe = new DictionaryEditor(true);
		pe.setAsText(null);
		assertNotNull(pe.getValue());
	}

	public void testNullIsJustNull() throws Exception {
		pe.setAsText(null);
		assertNull(pe.getValue());
	}

	public void testGetAsText() {
		assertNull(pe.getAsText());
		pe.setValue(new HashMap());
		assertNull(pe.getAsText());
	}

	public void testSetAsTextString() {
		try {
			pe.setAsText("boo");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

	}

	public void testSetValueMap() {
		Map map = new HashMap();
		map.put(new Object(), new Object());
		pe.setValue(map);
		assertTrue(pe.getValue() instanceof Dictionary);
		assertEquals(map, pe.getValue());
	}

	public void testSetValueDictionary() {
		Dictionary dict = new Hashtable();
		dict.put(new Object(), new Object());
		pe.setValue(dict);
		assertSame(dict, pe.getValue());
	}

}
