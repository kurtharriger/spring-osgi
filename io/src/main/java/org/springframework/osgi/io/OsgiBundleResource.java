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
package org.springframework.osgi.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Resource implementation for OSGi environments. 
 * <p/>
 * Lazy evaluation of the resource will be used.
 * 
 * Understands the "bundle:" resource prefix for explicit loading of resources
 * from the bundle. By default (no prefix or "bundle:" prefix) the resource is
 * localized just within the underlying bundle and attached fragments. For
 * classpath search, use
 * {@link org.springframework.core.io.ResourceLoader#CLASSPATH_URL_PREFIX}.
 * 
 * As fallback, the path is transformed to an URL thus supporting OSGi framework
 * specific prefixes.
 * 
 * @author Costin Leau
 * @author Adrian Colyer
 * 
 */
public class OsgiBundleResource extends AbstractResource {

	public static final String BUNDLE_URL_PREFIX = "bundle:";

	// public static final String BUNDLE_URL_URL_PREFIX = "bundle-url:";

	private static final char PREFIX_SEPARATOR = ':';

	private static final String ABSOLUTE_PATH_PREFIX = "/";

	private final Bundle bundle;

	private final String path;

	// helper field
	private final boolean hasBundlePrefix;

	public OsgiBundleResource(Bundle bundle, String path) {
		Assert.notNull(bundle, "Bundle must not be null");
		this.bundle = bundle;

		// check path
		Assert.notNull(path, "Path must not be null");

		this.path = StringUtils.cleanPath(path);

		hasBundlePrefix = this.path.startsWith(BUNDLE_URL_PREFIX);
	}

	/**
	 * Return the path for this resource.
	 */
	public final String getPath() {
		return path;
	}

	/**
	 * Return the bundle for this resource.
	 */
	public final Bundle getBundle() {
		return bundle;
	}

	/**
	 * This implementation opens an InputStream for the given URL. It sets the
	 * "UseCaches" flag to <code>false</code>, mainly to avoid jar file
	 * locking on Windows.
	 * 
	 * @see java.net.URL#openConnection()
	 * @see java.net.URLConnection#setUseCaches(boolean)
	 * @see java.net.URLConnection#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		URLConnection con = getURL().openConnection();
		con.setUseCaches(false);
		return con.getInputStream();
	}

	/**
	 * Locates the resource in the underlying bundle based on the prefix, if it
	 * exists. Note that the location happens per call since the classpath of
	 * the bundle for example can change during a bundle lifecycle (depending on
	 * its imports).
	 * 
	 * @see org.osgi.framework.Bundle#getEntry(String)
	 * @see org.osgi.framework.Bundle#getResource(String)
	 */
	public URL getURL() throws IOException {
		// TODO: does it make sense to cache the trimmed string to avoid the
		// workload on subsequent calls?
		URL url = null;

		String prefix = getPrefix(path);

		// no prefix is treated just like bundle:
		if (!StringUtils.hasText(prefix))
			prefix = BUNDLE_URL_PREFIX;

		// locate inside the bundle (bundle:)
		if (BUNDLE_URL_PREFIX.equals(prefix)) {
			url = getResourceFromBundle((hasBundlePrefix ? path.substring(BUNDLE_URL_PREFIX.length()) : path));
		}
		// locate inside the classpath (classpath:)
		else {
			if (path.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
				url = getResourceFromBundleClasspath(path.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length()));
			}
			else {
				// just try to convert it to an URL
				url = new URL(path);
			}
		}

		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}

		return url;
	}

	/**
	 * Resolves a resource from the filesystem.
	 * 
	 * @param fileName
	 * @return a URL to the returned resource or null if none is found
	 */
	protected URL getResourceFromFilesystem(String fileName) {
		File f = new File(fileName);
		if (!f.exists()) {
			return null;
		}
		try {
			return f.toURL();
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Resolves a resource from *this bundle only*. Only the bundle and its
	 * attached fragments are searched for the given resource.
	 * 
	 * @param bundlePath
	 * @return a URL to the returned resource or null if none is found
	 * 
	 * @see org.osgi.framework.Bundle#getEntry(String)
	 */
	protected URL getResourceFromBundle(String bundlePath) throws IOException {

		// ask nicely first
		URL url = bundle.getEntry(bundlePath);

		// let's ask again (workaround for KF which does not return URLs for
		// folders directly)
		if (url == null) {
			// ask for entries in the current folder
			Enumeration enm = bundle.findEntries(bundlePath, null, false);

			// get the first one and create the initial one
			if (enm != null && enm.hasMoreElements()) {
				URL foundURL = (URL) enm.nextElement();
				return new URL(foundURL, "./");
			}
		}

		// nothing found in the end
		return url;
	}

	/**
	 * Resolves a resource from the bundle's classpath. This will find resources
	 * in this bundle and also in imported packages from other bundles.
	 * 
	 * @param bundlePath
	 * @return a URL to the returned resource or null if none is found
	 * 
	 * @see org.osgi.framework.Bundle#getResource(String)
	 */
	protected URL getResourceFromBundleClasspath(String bundlePath) {
		return bundle.getResource(bundlePath);
	}

	/**
	 * Determine if the given path is relative or absolute.
	 * 
	 * @param locationPath
	 * @return
	 */
	protected boolean isRelativePath(String locationPath) {
		return ((locationPath.indexOf(PREFIX_SEPARATOR) == -1) && !locationPath.startsWith(ABSOLUTE_PATH_PREFIX));
	}

	/**
	 * This implementation creates a OsgiBundleResource, applying the given path
	 * relative to the path of the underlying resource of this descriptor.
	 * 
	 * @see org.springframework.util.StringUtils#applyRelativePath(String,
	 * String)
	 */
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new OsgiBundleResource(this.bundle, pathToUse);
	}

	/**
	 * This implementation returns the name of the file that this bundle path
	 * resource refers to.
	 * 
	 * @see org.springframework.util.StringUtils#getFilename(String)
	 */
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	/**
	 * This implementation returns a description that includes the bundle
	 * location.
	 */
	public String getDescription() {
		StringBuffer buf = new StringBuffer();
		buf.append("OSGi resource[");
		buf.append(this.path);
		buf.append("|id=");
		buf.append(bundle.getBundleId());
		buf.append("|symName=");
		buf.append(bundle.getSymbolicName());
		buf.append("]");

		return buf.toString();
	}

	/**
	 * This implementation compares the underlying bundle and path locations.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof OsgiBundleResource) {
			OsgiBundleResource otherRes = (OsgiBundleResource) obj;
			return (this.path.equals(otherRes.path) && ObjectUtils.nullSafeEquals(this.bundle, otherRes.bundle));
		}
		return false;
	}

	/**
	 * This implementation returns the hash code of the underlying class path
	 * location.
	 */
	public int hashCode() {
		return this.path.hashCode();
	}

	private String getPrefix(String path) {
		String EMPTY_PREFIX = "";
		String DELIMITER = ":";
		if (path == null)
			return EMPTY_PREFIX;
		int index = path.indexOf(DELIMITER);
		if (index > 0)
			// include :
			return path.substring(0, index + 1);
		return EMPTY_PREFIX;
	}
}
