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

import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

/**
 * Low level access API used for discovering the underlying platform
 * capabilities since there are subtle yet major differences between each
 * implementation.
 * 
 * @author Costin Leau
 * 
 */
public class RawIoTest extends BaseIoTest {

	private Bundle bundle;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.iandt.io.BaseIoTest#onSetUp()
	 */
	protected void onSetUp() throws Exception {
		bundle = getBundleContext().getBundle();
	}

	// don't use any extra bundles - just the test jar
	protected String[] getBundles() {
		return null;
	}

	public void testGetResourceOnMetaInf() throws Exception {
		URL url = bundle.getResource("/META-INF");
		System.out.println(url);
		assertNotNull(url);
	}

	// fails on Felix & KF
	public void testGetResourceOnRoot() throws Exception {
		URL url = bundle.getResource("/");
		System.out.println(url);
		assertNotNull(url);
	}

	// fails on Felix & KF
	public void testGetResourceSOnRoot() throws Exception {
		Enumeration enm = bundle.getResources("/");

		assertEquals("root folder not validated", 1, countEnumeration(enm));
	}

	// fails on KF
	public void testFindEntriesOnFolders() throws Exception {
		Enumeration enm = bundle.findEntries("/", null, false);
		// should get 3 entries - META-INF/, org/ and log4j.properties

		assertEquals("folders ignored", 3, countEnumeration(enm));
	}

	public void testFindEntriesOnSubFolders() throws Exception {
		Enumeration enm = bundle.findEntries("/META-INF", null, false);
		// should get 1 entry - META-INF/
		assertEquals("folders ignored", 1, countEnumeration(enm));
	}

	// Valid jars do not have entries for root folder / - in fact it doesn't
	// even exist
	// fails on KF
	public void testGetEntryOnRoot() throws Exception {
		URL url = bundle.getEntry("/");
		System.out.println(url);
		assertNotNull(url);
	}

	// get folders
	public void testGetEntriesShouldReturnFoldersOnRoot() throws Exception {
		Enumeration enm = bundle.getEntryPaths("/");

		assertEquals("folders ignored", 3, countEnumeration(enm));
	}

	public void testGetFolderEntry() throws Exception {
		URL url = bundle.getEntry("META-INF/");
		System.out.println(url);
		assertNotNull(url);
	}

	public void testGetFolderEntries() throws Exception {
		Enumeration enm = bundle.getEntryPaths("META-INF/");
		assertEquals("folders ignored", 1, countEnumeration(enm));
	}

	private int countEnumeration(Enumeration enm) {
		int count = 0;
		while (enm != null && enm.hasMoreElements()) {
			System.out.println(enm.nextElement());
			count++;
		}
		return count;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.ConditionalTestCase#isDisabledInThisEnvironment(java.lang.String)
	 */
	protected boolean isDisabledInThisEnvironment(String testMethodName) {
		return (!isEquinox());
	}

}
