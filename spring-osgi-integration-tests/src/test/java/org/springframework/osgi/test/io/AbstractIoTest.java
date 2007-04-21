/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.osgi.test.io;

import org.osgi.framework.Bundle;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.osgi.io.OsgiBundleResourceLoader;
import org.springframework.osgi.io.OsgiBundleResourcePatternResolver;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.util.ObjectUtils;

/**
 * Test to check if loading of files outside of the OSGi world (directly from
 * the filesystem is possible).
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractIoTest extends AbstractConfigurableBundleCreatorTests {

	private Resource thisClass;

	private ResourceLoader loader, defaultLoader;

	private ResourcePatternResolver patternLoader;

	protected void onSetUp() throws Exception {
		// load file using absolute path
		defaultLoader = new DefaultResourceLoader();
		thisClass = defaultLoader.getResource(AbstractIoTest.class.getName().replace('.', '/').concat(".class"));
		Bundle bundle = getBundleContext().getBundle();
		loader = new OsgiBundleResourceLoader(bundle);
		patternLoader = new OsgiBundleResourcePatternResolver(loader);
	}

	protected void onTearDown() throws Exception {
		thisClass = null;
	}

	public void testFileOutsideOSGi() throws Exception {
		String fileLocation = "file:///" + thisClass.getFile().getAbsolutePath();
		// use file system resource defaultLoader
		Resource fileResource = defaultLoader.getResource(fileLocation);
		assertTrue(fileResource.exists());

		// try loading the file using OsgiBundleResourceLoader
		Resource osgiResource = getResourceLoader().getResource(fileLocation);
		// check existence of the same file when loading through the
		// OsgiBundleRL
		// NOTE andyp -- we want this to work!!
		assertTrue(osgiResource.exists());

		assertEquals(fileResource.getURL(), osgiResource.getURL());
	}

	public void testNonExistentFileOutsideOSGi() throws Exception {
		String nonExistingLocation = thisClass.getURL().toExternalForm().concat("-bogus-extension");

		Resource nonExistingFile = defaultLoader.getResource(nonExistingLocation);
		assertNotNull(nonExistingFile);
		assertFalse(nonExistingFile.exists());

		Resource nonExistingFileOutsideOsgi = getResourceLoader().getResource(nonExistingLocation);
		assertNotNull(nonExistingFileOutsideOsgi);
		assertFalse(nonExistingFileOutsideOsgi.exists());
	}

	public void testClassPathFileLevelWildcardMatching() throws Exception {
		// find all classes
		Resource[] res = patternLoader.getResources("classpath:/org/springframework/osgi/test/io/AbstractIoTest.class");
		// at least two integration tests are available
		assertEquals(1, res.length);
	}
	public void testFileLevelWildcardMatching() throws Exception {
		// find all classes
		Resource[] res = patternLoader.getResources("bundle:/org/springframework/osgi/test/io/*.class");
		// at least two integration tests are available
		assertTrue(res.length > 1);
	}

	// fails on KF
	public void testFileLevelPatternMatching() throws Exception {
		// find just this class
		Resource[] res = patternLoader.getResources("bundle:/org/springframework/osgi/test/io/AbstractIo*.class");
		// should find only 1
		assertEquals(1, res.length);
	}
	public void testFileLevelCharPatternMatchingForOneChar() throws Exception {
		Resource[] res = patternLoader.getResources("bundle:org/springframework/osgi/test/io/AbstractIoTe*t.class");
		// should find only 1
		assertEquals(1, res.length);
	}


	// works on Equinox, Felix | fails on KF (which doesn't return AbstractIoTest.class)
	public void testFileLevelCharMatching() throws Exception {
		Resource[] res = patternLoader.getResources("bundle:org/springframework/osgi/test/io/AbstractIoTe?t.class");
		// should find only 1
		assertEquals(1, res.length);
	}

	public void testFileLevelDoubleCharMatching() throws Exception {
		Resource[] res = patternLoader.getResources("bundle:org/springframework/osgi/test/io/AbstractIoT??t.class");
		// should find only 1
		assertEquals(1, res.length);
	}


	// works on Equinox only
	public void tstFolderLevelWildcardMatching() throws Exception {
		// find all classes
		Resource[] res = patternLoader.getResources("bundle:/**/AbstractIoTest.class");
		// at least two integration tests are available
		assertEquals(1, res.length);
	}

	public void testNoPrefixMeansBundlePrefix() throws Exception {
		Resource[] wPrefix = patternLoader.getResources("bundle:**/*.class");
		Resource[] woPrefix = patternLoader.getResources("**/*.class");

		assertTrue(ObjectUtils.nullSafeEquals(wPrefix, woPrefix));
	}
}
