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
package org.springframework.osgi.test.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.osgi.test.storage.MemoryStorage;
import org.springframework.osgi.test.storage.Storage;
import org.springframework.util.StringUtils;

/**
 * Helper class for creating Jar files.
 * 
 * @author Costin Leau
 * 
 */
public class JarCreator {

	private static final Log log = LogFactory.getLog(JarCreator.class);

	public static final String CLASS_PATTERN = "/**/*.class";

	public static final String XML_PATTERN = "/**/*.xml";

	public static final String PROPS_PATTERN = "/**/*.properties";

	public static final String EVERYTHING_PATTERN = "/**/*";

	public static final String[] DEFAULT_CONTENT_PATTERN = new String[] { CLASS_PATTERN, XML_PATTERN, PROPS_PATTERN };

	private String TEST_CLASSES_DIR = "test-classes";

	private String[] contentPattern = DEFAULT_CONTENT_PATTERN;

	private ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();

	private Storage storage = new MemoryStorage();

	private String rootPath = determineRootPath();

	private boolean addFolders = true;

	/**
	 * Resources' root path (the root path does not become part of the jar).
	 * 
	 * @return the root path
	 */
	public String determineRootPath() {
		// load file using absolute path. This seems to be necessary in IntelliJ
		try {
			ResourceLoader fileLoader = new DefaultResourceLoader();
			Resource res = fileLoader.getResource(getClass().getName().replace('.', '/').concat(".class"));
			String fileLocation = "file://" + res.getFile().getAbsolutePath();
			fileLocation = fileLocation.substring(0, fileLocation.indexOf(TEST_CLASSES_DIR)) + TEST_CLASSES_DIR;
			if (res.exists()) {
				return fileLocation;
			}
		}
		catch (Exception e) {
		}

		return "file:./target/" + TEST_CLASSES_DIR;
	}

	/**
	 * Actual jar creation.
	 * 
	 * @return the number of bytes written to the underlying stream.
	 * 
	 * @throws Exception
	 */
	protected int addJarContent(Manifest manifest) throws IOException {
		int writtenBytes = 0;

		// load manifest
		// add it to the jar
		if (log.isTraceEnabled() && manifest != null)
			log.trace("adding MANIFEST.MF [" + manifest.getMainAttributes().entrySet() + "]");

		URL rootURL = new URL(rootPath);
		String rootPath = StringUtils.cleanPath(rootURL.getPath());

		Resource[][] resources = resolveResources();

		// removed duplicates
		Map entries = new TreeMap();

		// empty stream used for folders
		Resource folderResource = new ByteArrayResource(new byte[0]);

		// add folder entries also
		for (int i = 0; i < resources.length; i++) {
			for (int j = 0; j < resources[i].length; j++) {
				String relativeName = determineRelativeName(rootPath, resources[i][j]);
				entries.put(relativeName, resources[i][j]);

				if (addFolders) {
					int slash = relativeName.lastIndexOf('/');
					// ignore root folder
					if (slash > 1)
						entries.put(relativeName.substring(0, slash + 1), folderResource);
				}
			}
		}

		if (log.isTraceEnabled()) {
			log.trace("adding entries:");
			Set key = entries.keySet();
			for (Iterator iter = key.iterator(); iter.hasNext();) {
				log.trace(iter.next());
			}
		}

		JarOutputStream jarStream = null;

		try {
			// get the output stream
			OutputStream outputStream = storage.getOutputStream();

			// add a jar stream on top
			jarStream = (manifest != null ? new JarOutputStream(outputStream, manifest) : new JarOutputStream(
					outputStream));

			// add deps
			for (Iterator iter = entries.entrySet().iterator(); iter.hasNext();) {
				Map.Entry element = (Map.Entry) iter.next();
				// write jar entry
				writtenBytes += JarUtils.writeToJar((Resource) element.getValue(), (String) element.getKey(), jarStream);
			}
		}
		finally {
			try {
				jarStream.closeEntry();
			}
			catch (Exception ex) {
				// ignore
			}
			try {
				jarStream.finish();
			}
			catch (Exception ex) {
				// ignore
			}
			IOUtils.closeStream(jarStream);
		}

		return writtenBytes;
	}

	/**
	 * Create a jar using the current settings and return a {@link Resource}
	 * pointing to the jar.
	 * 
	 * @param manifest
	 */
	public Resource createJar(Manifest manifest) {
		try {
			addJarContent(manifest);
			return storage.getResource();
		}
		catch (IOException ex) {
			throw new RuntimeException("can't return input stream", ex);
		}
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
	private String determineRelativeName(String rootPath, Resource resource) throws IOException {
		String path = StringUtils.cleanPath(resource.getURL().toExternalForm());
		return path.substring(path.indexOf(rootPath) + rootPath.length());
	}

	/**
	 * Transform the pattern and rootpath into actual resources.
	 * 
	 * @return
	 * @throws Exception
	 */
	private Resource[][] resolveResources() throws IOException {
		ResourcePatternResolver resolver = getPatternResolver();

		String[] patterns = getContentPattern();
		Resource[][] resources = new Resource[patterns.length][];

		// transform Strings into Resources
		for (int i = 0; i < patterns.length; i++) {
			StringBuffer buffer = new StringBuffer(rootPath);
			buffer.append(patterns[i]);
			resources[i] = resolver.getResources(buffer.toString());
		}

		return resources;
	}

	/**
	 * @return Returns the contentPattern.
	 */
	public String[] getContentPattern() {
		return contentPattern;
	}

	/**
	 * Pattern for content matching. Note that using {@link #EVERYTHING_PATTERN}
	 * can become problematic on windows due to file system locking.
	 * 
	 * @param contentPattern The contentPattern to set.
	 */
	public void setContentPattern(String[] contentPattern) {
		this.contentPattern = contentPattern;
	}

	/**
	 * @return Returns the patternResolver.
	 */
	public ResourcePatternResolver getPatternResolver() {
		return patternResolver;
	}

	/**
	 * @param patternResolver The patternResolver to set.
	 */
	public void setPatternResolver(ResourcePatternResolver patternResolver) {
		this.patternResolver = patternResolver;
	}

	/**
	 * @return Returns the jarStorage.
	 */
	public Storage getStorage() {
		return storage;
	}

	/**
	 * @param jarStorage The jarStorage to set.
	 */
	public void setStorage(Storage jarStorage) {
		this.storage = jarStorage;
	}

	/**
	 * @param rootPath The rootPath to set.
	 */
	public String getRootPath(String rootPath) {
		return rootPath;
	}

	/**
	 * @param rootPath The rootPath to set.
	 */
	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	/**
	 * @return Returns the addFolders.
	 */
	public boolean isAddFolders() {
		return addFolders;
	}

	/**
	 * Whether the folders in which the files reside, should be added to the
	 * archive. Default is true since otherwise, the archive will contains only
	 * files and no folders.
	 * 
	 * @param addFolders The addFolders to set.
	 */
	public void setAddFolders(boolean addFolders) {
		this.addFolders = addFolders;
	}
}
