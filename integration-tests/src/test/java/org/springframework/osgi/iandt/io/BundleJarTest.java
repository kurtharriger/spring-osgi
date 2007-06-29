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
 * Bundle jar tests.
 * 
 * @author Costin Leau
 * 
 */
public class BundleJarTest extends BaseIoTest {

	public void testResourceFromJarOnly() throws Exception {
		Resource[] res = patternLoader.getResources("bundle-jar:/org/springframework/osgi/iandt/io/duplicate.file");
		assertEquals(1, res.length);
	}

	// fails under KF with StackOverflow since getEntryPaths() returns "" ?
	public void testResourceFromJarOnlyWithFolderLevelWildcard() throws Exception {
		Resource[] res = patternLoader.getResources("bundle-jar:/**/duplicat*.file");
		assertEquals(1, res.length);
	}

	public void testResourceFromFragmentsIgnored() throws Exception {
		Resource[] res = patternLoader.getResources("bundle-jar:/fragment*.file");
		assertEquals(0, res.length);

	}

	public void testResourceWithWildcardAtFileLevelFromFragmentsIgnored() throws Exception {
		Resource[] res = patternLoader.getResources("bundle-jar:/*.file");
		assertEquals(0, res.length);
	}

	// same as above
	public void testResourceWithWildcardAtFolderLevelFromFragmentsIgnored() throws Exception {
		Resource[] res = patternLoader.getResources("bundle-jar:/**/fragment*.file");
		assertEquals(0, res.length);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.ConditionalTestCase#isDisabledInThisEnvironment(java.lang.String)
	 */
	protected boolean isDisabledInThisEnvironment(String testMethodName) {
		return isKF();
	}
}
