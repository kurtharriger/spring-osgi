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

package org.springframework.osgi.web.extender.internal.scanner;

import java.net.URL;
import java.util.Enumeration;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.springframework.osgi.mock.ArrayEnumerator;
import org.springframework.osgi.mock.MockBundle;

/**
 * 
 * @author Costin Leau
 * 
 */
public class DefaultWarScannerTest extends TestCase {

	private Bundle bundle;
	private WarScanner scanner;


	protected void setUp() throws Exception {
		scanner = new DefaultWarScanner();
	}

	protected void tearDown() throws Exception {
		bundle = null;
		scanner = null;
	}

	public void testBundleWithExistingWebXml() throws Exception {
		final URL url = new URL("file:///");
		bundle = new MockBundle() {

			public Enumeration findEntries(String path, String filePattern, boolean recurse) {
				assertEquals("WEB-INF/", path);
				assertEquals("web.xml", filePattern);
				assertEquals(false, recurse);
				return new ArrayEnumerator(new URL[] { url });
			}
		};

		assertSame(url, scanner.getWebXmlConfiguration(bundle));
	}

	public void testBundleWithNoWebXML() throws Exception {
		bundle = new MockBundle() {

			public Enumeration findEntries(String path, String filePattern, boolean recurse) {
				return null;
			}
		};
		assertNull(scanner.getWebXmlConfiguration(bundle));
	}

	public void testBundleThatReturnsAnEmptyEnumeration() throws Exception {
		assertNull(scanner.getWebXmlConfiguration(new MockBundle()));
	}
}
