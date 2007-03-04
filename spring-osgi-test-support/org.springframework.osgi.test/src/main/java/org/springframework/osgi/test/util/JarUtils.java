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
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.springframework.core.io.Resource;

/**
 * Utility class for Jar files. As opposed to {@link JarCreator}, this class
 * contains only static methods (hence the abstract class).
 * 
 * 
 * @author Costin Leau
 * 
 */
public abstract class JarUtils {

	private static final int DEFAULT_BUFFER_SIZE = 1024;

	/**
	 * Dumps the content of a jar and returns it as a String.
	 * 
	 * @param jis
	 * @return
	 * @throws Exception
	 */
	public static String dumpJarContent(JarInputStream jis) {
		StringBuffer buffer = new StringBuffer();

		try {
			JarEntry entry;
			while ((entry = jis.getNextJarEntry()) != null) {
				buffer.append(entry.getName());
				buffer.append("\n");
			}
		}
		catch (IOException ioException) {
			buffer.append("reading from stream failed");
		}
		finally {
			IOUtils.closeStream(jis);
		}

		return buffer.toString();
	}

	public static String dumpJarContent(Resource resource) {
		try {
			return dumpJarContent(new JarInputStream(resource.getInputStream()));
		}
		catch (IOException ex) {
			return "reading from stream failed";
		}
	}

	/**
	 * Write a resource content to a jar.
	 * 
	 * @param res
	 * @param entryName
	 * @param jarStream
	 * @return the number of bytes written to the jar file
	 * @throws Exception
	 */
	public static int writeToJar(Resource res, String entryName, JarOutputStream jarStream) throws IOException {
		return writeToJar(res, entryName, jarStream, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * 
	 * Write a resource content to a jar.
	 * 
	 * @param res
	 * @param entryName
	 * @param jarStream
	 * @param bufferSize
	 * @return the number of bytes written to the jar file
	 * @throws Exception
	 */
	public static int writeToJar(Resource res, String entryName, JarOutputStream jarStream, int bufferSize)
			throws IOException {
		byte[] readWriteJarBuffer = new byte[bufferSize];

		// remove leading / if present.
		if (entryName.charAt(0) == '/')
			entryName = entryName.substring(1);

		jarStream.putNextEntry(new ZipEntry(entryName));
		InputStream entryStream = res.getInputStream();

		int numberOfBytes;

		// read data into the buffer which is later on written to the jar.
		while ((numberOfBytes = entryStream.read(readWriteJarBuffer)) != -1) {
			jarStream.write(readWriteJarBuffer, 0, numberOfBytes);
		}
		return numberOfBytes;
	}

}
