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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Enhanced subclass of AbstractOsgiTests which facilitates OSGi testing by
 * creating at runtime, on the fly, a jar using the indicated manifest and
 * resource patterns (by default all files found under the root path). Note that
 * more complex scenarios, dedicated packaging tools (such as ant scripts or
 * maven2) should be used.
 * <p>
 * 
 * @author Costin Leau
 * 
 */
public abstract class OnTheFlyBundleCreatorTests extends AbstractOsgiTests {

	File tempFile;
	// temporary buffer for reading various classes and then writing them to the
	// file
	private byte[] readWriteJarBuffer = new byte[1024];

	public OnTheFlyBundleCreatorTests() {
	}

	public OnTheFlyBundleCreatorTests(String testName) {
		super(testName);
	}

	/**
	 * Resources' root path (the root path does not become part of the jar).
	 * 
	 * @return the root path
	 */
	protected String getRootPath() {
		// load file using absolute path. This seems to be necessary in IntelliJ
		try {
			ResourceLoader fileLoader = new DefaultResourceLoader();
			Resource res = fileLoader.getResource(getClass().getName().replace('.', '/').concat(".class"));
			String fileLocation = "file://" + res.getFile().getAbsolutePath();
			fileLocation = fileLocation.substring(0, fileLocation.indexOf("test-classes")) + "test-classes";
			if (res.exists()) {
				return fileLocation;
			}
		}
		catch (IOException e) {
		}
		return "file:./target/test-classes";
	}

	/**
	 * Patterns for identifying the resources added to the jar. The patterns are
	 * added to the root path when performing the search.
	 * 
	 * @return the patterns
	 */
	protected String[] getBundleContentPattern() {
		return new String[] { "/**/*.class" };
	}

	/**
	 * Return the location (in Spring resource style) of the manifest location
	 * to be used.
	 * 
	 * @return the manifest location
	 */
	protected String getManifestLocation() {
		return "classpath:/org/springframework/osgi/test/MANIFEST.MF";
	}

	/**
	 * The pattern resolver used for loading resources.
	 * 
	 * @return
	 */
	protected ResourcePatternResolver getPatternResolver() {
		return new PathMatchingResourcePatternResolver();
	}

	private String dumpJarContent(JarInputStream jis) throws Exception {
		StringBuffer buffer = new StringBuffer();

		try {
			JarEntry entry;
			while ((entry = jis.getNextJarEntry()) != null) {
				buffer.append(entry.getName());
				buffer.append("\n");
			}
		}
		finally {
			jis.close();
		}

		return buffer.toString();
	}

	/**
	 * Write a resource content to a jar.
	 * 
	 * @param res
	 * @param entryName
	 * @param jarStream
	 * @throws Exception
	 */
	private void writeToJar(Resource res, String entryName, JarOutputStream jarStream) throws Exception {
		// remove leading / if present.
		if (entryName.charAt(0) == '/')
			entryName = entryName.substring(1);

		if (log.isDebugEnabled())
			log.debug("adding resource " + res.toString() + " under name " + entryName);
		jarStream.putNextEntry(new ZipEntry(entryName));
		InputStream entryStream = res.getInputStream();

		int numberOfBytes;

		// read data into the buffer which is later on written to the jar.
		while ((numberOfBytes = entryStream.read(readWriteJarBuffer)) != -1) {
			jarStream.write(readWriteJarBuffer, 0, numberOfBytes);
		}
	}

	/**
	 * Transform the pattern and rootpath into actual resources.
	 * 
	 * @return
	 * @throws Exception
	 */
	private Resource[][] resolveResources() throws Exception {
		ResourcePatternResolver resolver = getPatternResolver();

		String[] patterns = getBundleContentPattern();
		Resource[][] resources = new Resource[patterns.length][];

		// transform Strings into Resources
		for (int i = 0; i < patterns.length; i++) {
			StringBuffer buffer = new StringBuffer(getRootPath());
			buffer.append(patterns[i]);
			resources[i] = resolver.getResources(buffer.toString());
		}

		return resources;
	}

	protected Manifest getManifest() throws Exception {
		return new Manifest(getPatternResolver().getResource(getManifestLocation()).getInputStream());
	}

	/**
	 * Actual jar creation.
	 * 
	 * @throws Exception
	 */
	private void createJar() throws Exception {
		tempFile = File.createTempFile("spring.osgi", null);
		tempFile.deleteOnExit();

		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));

		// load manifest
		// add it to the jar
		if (log.isDebugEnabled())
			log.debug("adding MANIFEST.MF from location " + getPatternResolver().getResource(getManifestLocation()));
		JarOutputStream jarStream = new JarOutputStream(outputStream, getManifest());

		Resource[][] resources = resolveResources();
		URL rootURL = new URL(getRootPath());
		String rootPath = StringUtils.cleanPath(rootURL.getPath());

		// add deps
		for (int i = 0; i < resources.length; i++) {
			for (int j = 0; j < resources[i].length; j++) {
				// write the test
				writeToJar(resources[i][j], determineRelativeName(rootPath, resources[i][j]), jarStream);
			}
		}

		jarStream.finish();
		jarStream.closeEntry();
		jarStream.close();
	}

	/**
	 * Small utility method used for determining the file name by striping the
	 * root path from the file full path.
	 * 
	 * @param rootPath
	 * @param resource
	 * @return
	 * @throws Exception
	 */
	private String determineRelativeName(String rootPath, Resource resource) throws Exception {
		String path = StringUtils.cleanPath(resource.getURL().toExternalForm());
		return path.substring(path.indexOf(rootPath) + rootPath.length());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.AbstractOsgiTests#postProcessBundleContext(org.osgi.framework.BundleContext)
	 */
	protected void postProcessBundleContext(BundleContext context) throws Exception {
		log.debug("post processing: creating test bundle");

		// create the actual jar
		createJar();

		if (log.isTraceEnabled())
			log.trace("created jar:\n" + dumpJarContent(new JarInputStream(new FileInputStream(tempFile))));

		log.debug("installing bundle ");

		InputStream stream = new BufferedInputStream(new FileInputStream(tempFile));

		// install & start
		Bundle bundle;
		try {
			bundle = context.installBundle("[onTheFly-test-bundle]" + ClassUtils.getShortName(getClass()) + "[" + hashCode()
					+ "]", stream);
		}
		finally {
			stream.close();
		}

		log.debug("start bundle");
		bundle.start();
		log.debug("test bundle succesfully installed and started");
	}
}
