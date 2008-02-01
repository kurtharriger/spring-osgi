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

/**
 * @author Costin Leau
 * 
 */
public class ClassSpaceWildcardTest extends BaseIoTest {

	//
	// Wild-card tests
	//

	public void testWildcardAtRootFileLevel() throws Exception {
		Resource res[] = patternLoader.getResources("classpath:/*");
		// only the bundle and its fragments should be considered (since no other META-INF/ is available on the classpath)
		assertEquals("not enough packages found", 3, res.length);
	}

	// similar as the root test but inside META-INF
	public void testWildcardAtFolderLevel() throws Exception {
		Resource res[] = patternLoader.getResources("classpath:/META-INF/*");
		assertEquals("not enough packages found", 1, res.length);
	}

	public void testSingleClassWithWildcardAtFileLevel() throws Exception {
		Resource res[] = patternLoader.getResources("classpath:/org/springframework/osgi/iandt/io/Class*Test.class");
		assertTrue("not enough packages found", res.length >= 1);
	}

	public void testClassPathRootWildcard() throws Exception {
		Resource res[] = patternLoader.getResources("classpath:/**/osgi/iandt/io/Class*Test.class");
		assertTrue("not enough packages found", res.length >= 1);
	}

	public void testAllClassPathWildcardAtFolderLevel() throws Exception {
		Resource res[] = patternLoader.getResources("classpath*:/META-INF/*");
		// only the bundle and its fragments should be considered (since no other META-INF/ is available on the classpath)
		assertEquals("not enough packages found", 3, res.length);
	}

	public void testAllClassPathWOWildcardAtFolderLevel() throws Exception {
		Resource res[] = patternLoader.getResources("classpath*:/META-INF/");
		// only the bundle and its fragments should be considered (since no other META-INF/ is available on the classpath)
		assertEquals("not enough packages found", 3, res.length);
	}

	public void testAllClassPathWithWildcardAtFileLevel() throws Exception {
		Resource res[] = patternLoader.getResources("classpath:/org/springframework/osgi/iandt/io/Class*WildcardTest.class");
		assertEquals("not enough packages found", 1, res.length);
	}

	public void testAllClassPathWithWithWildcardAtFileLevel() throws Exception {
		Resource res[] = patternLoader.getResources("classpath*:/org/springframework/osgi/iandt/io/Class*WildcardTest.class");
		assertEquals("not enough packages found", 1, res.length);
	}

	public void testAllClassPathRootWildcard() throws Exception {
		Resource res[] = patternLoader.getResources("classpath:/**/springframework/osgi/**/ClassSpaceWildcardTest.class");
		assertEquals("not enough packages found", 1, res.length);
	}

	public void testAllClassPathRootWithWildcard() throws Exception {
		Resource res[] = patternLoader.getResources("classpath*:/**/springframework/osgi/**/ClassSpaceWildcardTest.class");
		assertEquals("not enough packages found", 1, res.length);
	}

	//
	// Stress tests (as they pull a lot of content)
	//

	public void testMatchingALotOfClasses() throws Exception {
		Resource res[] = patternLoader.getResources("classpath*:/**/springframework/osgi/iandt/io/*.class");
		// at least 2 classes should be in there
		assertTrue("not enough packages found", res.length > 1);
	}

	public void testMatchingALotOfFolders() throws Exception {
		Resource res[] = patternLoader.getResources("classpath*:/**/springframework/osgi/**");
		assertTrue("not enough packages found", res.length > 10);
	}

	// ask for everything springframework :)
	// normally around 147+ resources are found
	public void testMatchingABulkOfResources() throws Exception {
		Resource res[] = patternLoader.getResources("classpath*:/**/springframework/**");
		assertTrue("not enough packages found", res.length > 50);
	}

	// ask for everything org :)
	// normally around 250+ resources are found
	public void testMatchingAHugeSetOfResources() throws Exception {
		Resource res[] = patternLoader.getResources("classpath*:/org/**");
		assertTrue("not enough packages found", res.length > 100);
	}

	// disabled some tests on KF
	protected boolean isDisabledInThisEnvironment(String testMethodName) {
		return isKF()
				&& (testMethodName.equals("testMatchingABulkOfResources")
						|| testMethodName.equals("testMatchingABulkOfResources") || testMethodName.equals("testAllClassPathRootWithWildcard"));
	}
}
