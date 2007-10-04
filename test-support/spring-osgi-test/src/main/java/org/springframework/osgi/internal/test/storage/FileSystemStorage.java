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
package org.springframework.osgi.internal.test.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * File-system based storage. Uses a temporary file for storing information.
 * 
 * @author Costin Leau
 * 
 */
public class FileSystemStorage implements Storage {

	private File storage;

	private static final String TEMP_FILE_PREFIX = "spring.osgi";

	public FileSystemStorage() {
		try {
			storage = File.createTempFile(TEMP_FILE_PREFIX, null);
		}
		catch (IOException ex) {
			throw new RuntimeException("cannot create temporary file", ex);
		}
		storage.deleteOnExit();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.test.util.Storage#getInputStream()
	 */
	public InputStream getInputStream() {
		try {
			return new BufferedInputStream(new FileInputStream(storage));
		}
		catch (IOException ex) {
			throw new RuntimeException("cannot return file stream", ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.test.util.Storage#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		try {
			return new BufferedOutputStream(new FileOutputStream(storage));
		}
		catch (IOException ex) {
			throw new RuntimeException("cannot return file stream", ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.test.util.Storage#dispose()
	 */
	public void dispose() {
		storage.delete();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.test.util.Storage#getResource()
	 */
	public Resource getResource() {
		return new FileSystemResource(storage);
	}
}
