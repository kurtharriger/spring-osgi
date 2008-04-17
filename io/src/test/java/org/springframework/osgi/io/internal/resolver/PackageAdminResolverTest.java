/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.io.internal.resolver;

import junit.framework.TestCase;

import org.springframework.osgi.io.internal.resolver.PackageAdminResolver;
import org.springframework.osgi.mock.MockBundleContext;

public class PackageAdminResolverTest extends TestCase {

	private PackageAdminResolver paResolver;
	private static String pkg = "com.acme.facade";
	private static String defaultVersion = "0.0.0";


	protected void setUp() throws Exception {
		paResolver = new PackageAdminResolver(new MockBundleContext());
	}

	protected void tearDown() throws Exception {
		paResolver = null;
	}

	public void testParseEntryWithAttribute() throws Exception {
		String[] values = paResolver.parseRequiredBundleString(pkg + ";visibility:=reexport");
		assertEquals(pkg, values[0]);
		assertEquals(defaultVersion, values[1]);
	}

	public void testParseSimpleEntry() throws Exception {
		String[] values = paResolver.parseRequiredBundleString(pkg);
		assertEquals(pkg, values[0]);
		assertEquals(defaultVersion, values[1]);
	}

	public void testParseEntryWithSingleVersion() throws Exception {
		String[] values = paResolver.parseRequiredBundleString(pkg + ";bundle-version=\"1.0\"");
		assertEquals(pkg, values[0]);
		assertEquals("1.0", values[1]);
	}

	public void testParseEntryWithRangeVersion() throws Exception {
		String[] values = paResolver.parseRequiredBundleString(pkg + ";bundle-version=\"[\"1.0,2.0\")\"");
		assertEquals(pkg, values[0]);
		assertEquals("[1.0,2.0)", values[1]);
	}

	public void testParseEntryWithRangeVersionAndExtraHeader() throws Exception {
		String[] values = paResolver.parseRequiredBundleString(pkg
				+ ";bundle-version=\"[\"1.0,2.0\")\";visibility:=reexport");
		assertEquals(pkg, values[0]);
		assertEquals("[1.0,2.0)", values[1]);
	}

	public void testParseEntryWithExtraHeaderAndRangeVersion() throws Exception {
		String[] values = paResolver.parseRequiredBundleString(pkg
				+ ";visibility:=reexport;bundle-version=\"[\"1.0,2.0\")\"");
		assertEquals(pkg, values[0]);
		assertEquals("[1.0,2.0)", values[1]);
	}

	public void testParseEntryWithExtraHeaderAndSimpleVersion() throws Exception {
		String[] values = paResolver.parseRequiredBundleString(pkg + ";visibility:=reexport;bundle-version=\"1.0\"");
		assertEquals(pkg, values[0]);
		assertEquals("1.0", values[1]);
	}

}
