/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.web.extender.internal.jetty;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.mortbay.resource.Resource;
import org.mortbay.resource.URLResource;
import org.mortbay.util.URIUtil;
import org.osgi.framework.Bundle;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Jetty specific resource wrapped providing access to an OSGi bundle space.
 * 
 * 
 * @author Costin Leau
 * 
 */
// TODO: does it make sense to use the Spring IO API instead?
public class BundleSpaceJettyResource extends URLResource {

	private static final String FOLDER_SEPARATOR = "/";
	private final Bundle bundle;
	private final String path;
	// top level path (a/b/c -> a/b/)
	private final String pathTreeEntry;
	// last path entry (/a/b/c -> c) 
	private final String pathLeafEntry;


	private static URL craftURL(Bundle bundle, String path) {
		Assert.notNull(bundle);
		Assert.notNull(path);

		URL url = bundle.getResource(FOLDER_SEPARATOR);

		if (path.startsWith(FOLDER_SEPARATOR))
			path = path.substring(1, path.length());
		try {
			url = new URL(url.toExternalForm() + path);
		}
		catch (MalformedURLException ex) {
			throw (RuntimeException) new IllegalArgumentException("invalid url " + path).initCause(ex);
		}
		return url;
	}

	public BundleSpaceJettyResource(Bundle bundle, String path) {
		super(craftURL(bundle, path), null, false);

		this.bundle = bundle;
		this.path = path;

		String tempPath = path;
		// check if the path is a folder so it can
		// broken down even further
		if (tempPath.endsWith(FOLDER_SEPARATOR)) {
			tempPath = tempPath.substring(0, tempPath.length() - 1);
		}

		// dis-assemble the path into folders and file (useful when checking
		// for the existence of the resource
		pathLeafEntry = StringUtils.getFilename(tempPath);
		pathTreeEntry = (pathLeafEntry == null ? null : tempPath.substring(0, tempPath.length() - pathLeafEntry.length()));
	}

	public File getFile() throws IOException {
		// the bundle location cannot be retrieved inside the OSGi space
		return null;
	}

	public String[] list() {
		Enumeration enm = bundle.findEntries(path, null, false);

		List list = new ArrayList(8);

		while (enm != null && enm.hasMoreElements()) {
			URL entry = (URL) enm.nextElement();
			// get only the path not the entire URL
			String entryPath = entry.getPath();

			// remove base path
			// and trailing "/" for folders to resemble File#list()
			if (entryPath.endsWith(FOLDER_SEPARATOR))
				entryPath = entryPath.substring(0, entryPath.length() - 1);
			list.add(entryPath.substring(path.length()));
		}
		return (String[]) list.toArray(new String[list.size()]);
	}

	// override to avoid the static call which returns an URL resource
	public Resource addPath(String path) throws IOException, MalformedURLException {
		if (path == null)
			return null;

		path = URIUtil.canonicalPath(path);
		String newPath = URIUtil.addPaths(this.path, path);

		return new BundleSpaceJettyResource(bundle, newPath);
	}

	public boolean exists() {
		Enumeration enm = bundle.findEntries(pathTreeEntry, pathLeafEntry, false);
		return (enm != null && enm.hasMoreElements());
	}

	public boolean isDirectory() {
		return exists() && path.endsWith(FOLDER_SEPARATOR);
	}

}
