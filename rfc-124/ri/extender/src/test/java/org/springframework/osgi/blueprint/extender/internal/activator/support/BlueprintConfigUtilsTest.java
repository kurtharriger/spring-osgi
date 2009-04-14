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

package org.springframework.osgi.blueprint.extender.internal.activator.support;

import java.util.Dictionary;
import java.util.Properties;

import org.springframework.beans.propertyeditors.CharacterEditor;
import org.springframework.util.ObjectUtils;

import junit.framework.TestCase;

/**
 * Test intended mainly for checking the header parsing.
 * 
 * @author Costin Leau
 */
public class BlueprintConfigUtilsTest extends TestCase {

	private Dictionary headers;
	private String dir1 = "foo:=bar", dir2 = "ignored-directive:=true", dir3 = "version=123";
	private String loc1 = "OSGI-INF/foo.xml", loc2 = "/META-INF/spring/context.xml";


	@Override
	protected void setUp() throws Exception {
		headers = new Properties();
	}

	@Override
	protected void tearDown() throws Exception {
		headers = null;
	}

	private String[] getLocations(String location) {
		headers.put(BlueprintConfigUtils.BLUEPRINT_HEADER, location);
		return BlueprintConfigUtils.getBlueprintHeaderLocations(headers);
	}

	public void testNoLocation() throws Exception {
		String[] locs = getLocations(dir1 + ";" + dir3);
		assertTrue(ObjectUtils.isEmpty(locs));
	}

	public void testOneLocationWithNoDirective() throws Exception {
		String[] locs = getLocations(loc1);
		assertEquals(1, locs.length);
		assertEquals(loc1, locs[0]);
	}

	public void testOneLocationWithOneDirective() throws Exception {
		String[] locs = getLocations(loc1 + ";" + dir1);
		assertEquals(1, locs.length);
		assertEquals(loc1, locs[0]);
	}

	public void testOneLocationWithMultipleDirectives() throws Exception {
		String[] locs = getLocations(dir2 + ";" + dir3 + ";" + loc2 + ";" + dir1 + ";" + dir2);
		assertEquals(1, locs.length);
		assertEquals(loc2, locs[0]);
	}

	public void testMultipleLocationsWODirectives() throws Exception {
		String[] locs = getLocations(loc1 + "," + loc2);
		assertEquals(2, locs.length);
		assertEquals(loc1, locs[0]);
		assertEquals(loc2, locs[1]);
	}

	public void testMultipleLocationsWithMultipleDirectives() throws Exception {
		String[] locs = getLocations(dir1 + ";" + loc1 + ";" + dir2 + "," + dir3 + ";" + loc2);
		assertEquals(2, locs.length);
		assertEquals(loc1, locs[0]);
		assertEquals(loc2, locs[1]);
	}
	
	public void testUnicodeChars() throws Exception {
		CharacterEditor editor = new CharacterEditor(false);
		editor.setAsText("\\u2122");
		System.out.println(editor.getAsText());
	}
}