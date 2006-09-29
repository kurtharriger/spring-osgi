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

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import junit.framework.TestCase;

import org.springframework.util.ReflectionUtils;

/**
 * @author Costin Leau
 * 
 */
public class OnTheFlyBundleCreatorTestsTest extends TestCase {

	public void testJarCreation() throws Exception {

		final Manifest mf = new Manifest();

		Map entries = mf.getEntries();
		Attributes attrs = new Attributes();

		attrs.putValue("rocco-ventrella", "winelight");
		entries.put("test", attrs);

		String location = OnTheFlyBundleCreatorTestsTest.class.getName().replace('.', '/') + ".class";
		final URL clazzURL = getClass().getClassLoader().getResource(location);

		OnTheFlyBundleCreatorTests test = new OnTheFlyBundleCreatorTests() {

			protected Manifest getManifest() throws Exception {
				return mf;
			}

			protected String getRootPath() {
				return "file:";
			}

			protected String[] getBundleContentPattern() {
				return new String[] { clazzURL.getFile() };
			}
		};

		Method method = ReflectionUtils.findMethod(OnTheFlyBundleCreatorTests.class, "createJar", new Class[] {});
		method.setAccessible(true);
		ReflectionUtils.invokeMethod(method, test);

		// get temp file
		Field fileField = OnTheFlyBundleCreatorTests.class.getDeclaredField("tempFile");
		fileField.setAccessible(true);
		File jarFile = (File) fileField.get(test);

		// start reading the jar
		JarFile jar = new JarFile(jarFile);
		assertEquals(mf, jar.getManifest());
		Enumeration enum = jar.entries();
		enum.nextElement();
		InputStream jarContent = jar.getInputStream(jar.getEntry("file:"+clazzURL.getFile()));
		InputStream originalFile = clazzURL.openStream();

		try {
			int b;
			while ((b = originalFile.read()) != -1)
				assertEquals(b, jarContent.read());
		}
		finally {
			if (jarContent != null)
				jarContent.close();
			if (originalFile != null)
				originalFile.close();
		}
	}
}
