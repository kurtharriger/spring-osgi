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
package org.springframework.osgi.iandt.io;

import org.springframework.core.io.Resource;
import org.springframework.util.ObjectUtils;

/**
 * Classpath tests.
 * 
 * @author Costin Leau
 * 
 */
public class ClassSpaceTest extends BaseIoTest {

	public void testFolder() throws Exception {
		Resource res[] = patternLoader.getResources("classpath:/org/springframework/osgi");
		// EQ returns the fragments paths also
		assertTrue(res.length >= 1);
	}

	// META-INF seems to be a special case, since the manifest is added
	// automatically by the jar stream
	// but it's the JarCreator which creates the META-INF folder
	public void testMetaInfFolder() throws Exception {
		Resource res[] = patternLoader.getResources("classpath:/META-INF");
		// Equinox returns more entries (bootpath delegation)
		assertTrue(res.length >= 1);
	}

	public void testWildcardAtFolderLevel() throws Exception {
		try {
			Resource res[] = patternLoader.getResources("classpath:/META-INF/*");
			fail("should have thrown exception; pattern matching is not supported for class space lookups");
		}
		catch (IllegalArgumentException e) {
			// expected
		}

	}

	public void testClass() throws Exception {
		Resource res[] = patternLoader.getResources("classpath:/org/springframework/osgi/iandt/io/ClassSpaceTest.class");
		assertEquals(1, res.length);
	}

	public void testSingleClassWithWildcardAtFileLevel() throws Exception {
		try {
			Resource res[] = patternLoader.getResources("classpath:/**/io/**/Class*Test.class");
			fail("should have thrown exception");
		}
		catch (RuntimeException ex) {
			// expected
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.ConditionalTestCase#isDisabledInThisEnvironment(java.lang.String)
	 */
	protected boolean isDisabledInThisEnvironment(String testMethodName) {
		return isKF() && ("testFolder".equals(testMethodName) || "testMetaInfFolder".equals(testMethodName));
	}
}
