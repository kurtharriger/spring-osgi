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
 * IO tests related to fragments.
 * 
 * @author Costin Leau
 * 
 */
public class FragmentIoTests extends BaseIoTest {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.ConditionalTestCase#isDisabledInThisEnvironment(java.lang.String)
	 */
	protected boolean isDisabledInThisEnvironment(String testMethodName) {
		return (!isEquinox);
	}

	public void testFileWithTheSameNameInOwningBundleAndAttachedFragment() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/" + PACKAGE + "duplicate.file");
		assertTrue("file with the same name avaiable in bundles are ignored", fragmentResource.length > 1);
	}

	public void testFileWithTheSameNameInOwningBundleAndAttachedFragmentWithMatchingAtFileLevel() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/" + PACKAGE + "dupli*.file");
		assertTrue("file with the same name avaiable in bundles are ignored", fragmentResource.length > 1);
	}

	public void testFileWithTheSameNameInOwningBundleAndAttachedFragmentWithMatchingAtFileLevelOn1Char()
			throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/" + PACKAGE + "duplic?te.file");
		assertTrue("file with the same name avaiable in bundles are ignored", fragmentResource.length > 1);
	}

	public void testFileWithTheSameNameInOwningBundleAndAttachedFragmentWithMatchingAtFolderLevel() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/org/springframework/**/*.file");
		assertTrue("file with the same name avaiable in bundles are ignored", fragmentResource.length > 1);
	}

	public void testFileLevelWildCardMatchingIncludingFragmentResources() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/" + PACKAGE + "*.file");

		// 1 in each attached bundle
		assertTrue(fragmentResource.length > 1);
	}

	public void testFileLevelMatchingJustFragments() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/fragment*.file");

		// we have at least one duplicates in two bundles
		assertTrue(fragmentResource.length > 1);
	}

	public void testFileOnlyInOneFragment() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/fragment.file");
		assertEquals(1, fragmentResource.length);
	}

	public void testFileOnlyInAnotherFragment() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/fragment2.file");
		assertEquals(1, fragmentResource.length);
	}

	public void testFileWithTheSameNameOnlyInAttachedFragmentsOnly() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/" + PACKAGE + "fragment-duplicate.file");
		// should find at least 2
		assertTrue("files with the same name avaiable in attached fragments are ignored", fragmentResource.length > 1);
	}

	public void testFileWithTheSameNameOnlyInAttachedFragmentsOnlyWithMatchingAtFileLevel() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/" + PACKAGE + "fragment-dupli*.file");
		// should find at least 2
		assertTrue("files with the same name avaiable in attached fragments are ignored", fragmentResource.length > 1);
	}

	public void testFileWithTheSameNameOnlyInAttachedFragmentsOnlyWithMatchingAtFileLevelOnOneChar() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/" + PACKAGE + "fragment-duplic?t?.file");
		// should find at least 2
		assertTrue("files with the same name avaiable in attached fragments are ignored", fragmentResource.length > 1);
	}

	public void testFileWithTheSameNameOnlyInAttachedFragmentsOnlyWithMatchingAtFolderLevel() throws Exception {
		Resource[] fragmentResource = patternLoader.getResources("bundle:/org/springframework/**/fragment-duplicate.file");
		// should find at least 2
		assertTrue("files with the same name avaiable in attached fragments are ignored", fragmentResource.length > 1);
	}

	public void testGetRootInBundleAndFragments() throws Exception {
		Resource[] res = patternLoader.getResources("/");
		assertTrue(res.length > 1);
		res = patternLoader.getResources("bundle:/");
		assertTrue(res.length > 1);
	}

	public void testFileLevelWildcardMatching() throws Exception {
		// find res files
		Resource[] res = patternLoader.getResources("bundle:/" + PACKAGE + "*.res");
		assertEquals(2, res.length);
	}

	public void testFileLevelPatternMatching() throws Exception {
		// find just this class
		Resource[] res = patternLoader.getResources("bundle:/" + PACKAGE + "IoTe*.class");
		// should find only 1
		assertEquals(1, res.length);
	}

	public void testFileLevelCharPatternMatchingForOneChar() throws Exception {
		Resource[] res = patternLoader.getResources("bundle:" + PACKAGE + "IoTe*ts.class");
		// should find only 1
		assertEquals(1, res.length);
	}

	public void testFileLevelCharMatching() throws Exception {
		Resource[] res = patternLoader.getResources("bundle:" + PACKAGE + "IoTe?ts.class");
		// should find only 1
		assertEquals(1, res.length);
	}

	public void testFileLevelDoubleCharMatching() throws Exception {
		Resource[] res = patternLoader.getResources("bundle:" + PACKAGE + "IoTe??s.class");
		// should find only 1
		assertEquals(1, res.length);
	}

	public void testFolderLevelWildcardMatching() throws Exception {
		// find all classes
		Resource[] res = patternLoader.getResources("bundle:/**/io/*.class");
		assertTrue(res.length > 3);
	}

	public void testNoPrefixMeansBundlePrefixOnClasses() throws Exception {
		Resource[] wPrefix = patternLoader.getResources("bundle:**/*.class");
		Resource[] woPrefix = patternLoader.getResources("**/*.class");

		assertTrue(ObjectUtils.nullSafeEquals(wPrefix, woPrefix));
	}

	public void testNoPrefixMeansBundlePrefixOnFiles() throws Exception {
		Resource[] wPrefix = patternLoader.getResources("bundle:**/*.res");
		Resource[] woPrefix = patternLoader.getResources("**/*.res");

		assertTrue(ObjectUtils.nullSafeEquals(wPrefix, woPrefix));
	}

	public void testClassPathFileLevelMatching() throws Exception {
		// find this classe
		Resource[] res = patternLoader.getResources("classpath:/" + PACKAGE + "FragmentIoTests.class");
		assertEquals(1, res.length);
	}
}
