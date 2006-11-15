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
package org.springframework.osgi.test.integration;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;




/**
 * Test to check if loading of files outside of the OSGi world (directly from
 * the filesystem is possible).
 * 
 * @author Costin Leau
 * 
 */
public class FileSystemAccessTest extends ConfigurableBundleCreatorTests {

	public void testFileOutsideOSGi() throws Exception {
		// load file using absolute path
		ResourceLoader fileLoader = new DefaultResourceLoader();
		Resource res = fileLoader.getResource(FileSystemAccessTest.class.getName().replace('.', '/').concat(".class"));
		String fileLocation = "file://" + res.getFile().getAbsolutePath();
		// use file system resource loader
		Resource fileResource = fileLoader.getResource(fileLocation);
		assertTrue(fileResource.exists());
		// try loading the file using OsgiBundleResourceLoader
		Resource osgiResource = getResourceLoader().getResource(fileLocation);
		// check existence of the same file when loading through the OsgiBundleRL
		assertFalse(osgiResource.exists());
	}
}
