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
package org.springframework.osgi.test;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import junit.framework.TestCase;

import org.springframework.osgi.test.storage.FileSystemStorage;
import org.springframework.osgi.test.storage.Storage;
import org.springframework.osgi.test.util.IOUtils;
import org.springframework.osgi.test.util.JarCreator;

/**
 * @author Costin Leau
 * 
 */
public class JarCreatorTests extends TestCase {

	private JarCreator creator;

	private Storage storage;

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		creator = new JarCreator();
		storage = new FileSystemStorage();
		creator.setStorage(storage);
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		storage.dispose();
	}

	public void testJarCreation() throws Exception {

		final Manifest mf = new Manifest();

		Map entries = mf.getEntries();
		Attributes attrs = new Attributes();

		attrs.putValue("rocco-ventrella", "winelight");
		entries.put("test", attrs);

		String location = JarCreatorTests.class.getName().replace('.', '/') + ".class";
		final URL clazzURL = getClass().getClassLoader().getResource(location);

		// create a simple jar from a given class and a manifest
		creator.setContentPattern(new String[] { clazzURL.getFile() });
		creator.setRootPath("file:");
		
		// create the jar
		creator.createJar(mf);
		

		// start reading the jar
		JarInputStream jarStream = null;

		try {
			jarStream = new JarInputStream(storage.getInputStream());
			// get manifest
			assertEquals(mf, jarStream.getManifest());
			
			// move the jar stream to the file content
			jarStream.getNextEntry();
			// open the original file
			InputStream originalFile = clazzURL.openStream();

			int b;
			while ((b = originalFile.read()) != -1)
				assertEquals("incorrect jar content", b, jarStream.read());
		}
		finally {
			IOUtils.closeStream(jarStream);

		}
	}
}
