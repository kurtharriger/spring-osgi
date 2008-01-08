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
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.osgi.io.internal.OsgiResourceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Resource implementation for OSGi environments.
 * 
 * <p/> Lazy evaluation of the resource will be used.
 * 
 * This implementation allows resource location inside:
 * 
 * <ul>
 * <li>bundle space - if {@link #BUNDLE_URL_PREFIX} prefix is being used or
 * none is specified</li>
 * <li>bundle jar - if {@link #BUNDLE_JAR_URL_PREFIX} is specified</li>
 * <li>class space - if {@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX} is
 * encountered</li>
 * </ul>
 * 
 * As fall back, the path is transformed to an URL thus supporting the underlying
 * OSGi framework specific prefixes.
 * 
 * Note that when the bundle space (bundle jar and its attached fragments) is
 * being searched, multiple URLs can be found but this implementation will
 * return only the first one. Consider using
 * {@link OsgiBundleResourcePatternResolver} to retrieve all entries.
 * 
 * @author Costin Leau
 * @author Adrian Colyer
 * 
 */
public class OsgiBundleResource extends AbstractResource {

	/**
	 * Prefix for searching inside the owning bundle space. This translates to
	 * searching the bundle and its attached fragments. If no prefix is
	 * specified, this one will be used.
	 */
	public static final String BUNDLE_URL_PREFIX = "osgibundle:";

	/**
	 * Prefix for searching only the bundle raw jar. Will ignore attached
	 * fragments. Not used at the moment.
	 */
	public static final String BUNDLE_JAR_URL_PREFIX = "osgibundlejar:";

	private static final char PREFIX_SEPARATOR = ':';

	private static final String ABSOLUTE_PATH_PREFIX = "/";

	private final Bundle bundle;

	private final String path;

	// used to avoid removing the prefix every time the URL is required
	private String pathWithoutPrefix;

	// Bundle resource possible searches

	private int searchType = OsgiResourceUtils.PREFIX_TYPE_NOT_SPECIFIED;

	public OsgiBundleResource(Bundle bundle, String path) {
		Assert.notNull(bundle, "Bundle must not be null");
		this.bundle = bundle;

		// check path
		Assert.notNull(path, "Path must not be null");

		this.path = StringUtils.cleanPath(path);

		this.searchType = OsgiResourceUtils.getSearchType(this.path);
	}

	/**
	 * Return the path for this resource.
	 */
	final String getPath() {
		return path;
	}

	/**
	 * Return the bundle for this resource.
	 */
	final Bundle getBundle() {
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
		URL url = null;

		switch (searchType) {
		// same as bundle space but with a different string
		case OsgiResourceUtils.PREFIX_TYPE_NOT_SPECIFIED:
			if (pathWithoutPrefix == null)
				pathWithoutPrefix = path;
			url = getResourceFromBundleSpace(pathWithoutPrefix);
			break;
		case OsgiResourceUtils.PREFIX_TYPE_BUNDLE_SPACE:
			if (pathWithoutPrefix == null)
				pathWithoutPrefix = path.substring(BUNDLE_URL_PREFIX.length());
			url = getResourceFromBundleSpace(pathWithoutPrefix);
			break;
		case OsgiResourceUtils.PREFIX_TYPE_BUNDLE_JAR:
			if (pathWithoutPrefix == null)
				pathWithoutPrefix = path.substring(BUNDLE_JAR_URL_PREFIX.length());

			url = getResourceFromBundleJar(pathWithoutPrefix);
			break;
		case OsgiResourceUtils.PREFIX_TYPE_CLASS_SPACE:
			if (pathWithoutPrefix == null)
				pathWithoutPrefix = path.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length());

			url = getResourceFromBundleClasspath(pathWithoutPrefix);
			break;
		// fallback
		default:
			// just try to convert it to an URL
			url = new URL(path);
			break;
		}

		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}

		return url;
	}

	/**
	 * Resolves a resource from the file system.
	 * 
	 * @param fileName
	 * @return a URL to the returned resource or null if none is found
	 */
	URL getResourceFromFilesystem(String fileName) {
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
	 * Resolves a resource from *the bundle space* only. Only the bundle and its
	 * attached fragments are searched for the given resource. Note that this
	 * method returns only the first URL found, discarding the rest. To retrieve
	 * the entire set, consider using {@link OsgiBundleResourcePatternResolver}.
	 * 
	 * @param bundlePath the path to resolve
	 * @return a URL to the returned resource or null if none is found
	 * @throws IOException
	 * 
	 * @see {@link org.osgi.framework.Bundle#findEntries(String, String, boolean)}
	 */
	URL getResourceFromBundleSpace(String bundlePath) throws IOException {
		URL[] res = getAllUrlsFromBundleSpace(bundlePath);
		return (ObjectUtils.isEmpty(res) ? null : res[0]);
	}

	/**
	 * Resolves a resource from the *bundle jar* only. Only the bundle jar is
	 * searched (its attached fragments are ignored).
	 * 
	 * @param bundlePath the path to resolve
	 * @return URL to the specified path or null if none is found
	 * @throws IOException
	 * 
	 * @see {@link Bundle#getEntry(String)}
	 */
	URL getResourceFromBundleJar(String bundlePath) throws IOException {
		return bundle.getEntry(bundlePath);
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
	URL getResourceFromBundleClasspath(String bundlePath) {
		return bundle.getResource(bundlePath);
	}

	/**
	 * Determine if the given path is relative or absolute.
	 * 
	 * @param locationPath
	 * @return
	 */
	boolean isRelativePath(String locationPath) {
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

    public File getFile() throws IOException {
        if (searchType != OsgiResourceUtils.PREFIX_TYPE_UNKNOWN) {
            return super.getFile();
        }
        try {
            URL url = new URL(path);
            return new File(url.getPath());
        } catch (MalformedURLException mue) {
            throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
        }
    }

    /**
	 * This implementation returns a description that includes the bundle
	 * location.
	 */
	public String getDescription() {
		StringBuffer buf = new StringBuffer();
		buf.append("OSGi resource[");
		buf.append(this.path);
		buf.append("|bnd.id=");
		buf.append(bundle.getBundleId());
		buf.append("|bnd.sym=");
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

	/**
	 * @return Returns the searchType.
	 */
	int getSearchType() {
		return searchType;
	}

	/**
	 * Used internally to get all the URLs matching a certain location.
	 * The method is required to extract the folder from the given location 
	 * as well the file.
	 * 
	 * @param location location to look for
	 * @return an array of URLs
	 * @throws IOException
	 */
	URL[] getAllUrlsFromBundleSpace(String location) throws IOException {
		if (bundle == null)
			throw new IllegalArgumentException(
					"cannot locate items in bundle-space w/o a bundle; specify one when creating this resolver");

		Assert.notNull(location);
		Set resources = new LinkedHashSet(5);

		location = StringUtils.cleanPath(location);
		location = OsgiResourceUtils.stripPrefix(location);

		if (!StringUtils.hasText(location))
			location = OsgiResourceUtils.FOLDER_DELIMITER;

		// the root folder is requested (special case
		if (OsgiResourceUtils.FOLDER_DELIMITER.equals(location)) {
			Enumeration candidates = bundle.findEntries(location, null, false);

			while (candidates != null && candidates.hasMoreElements()) {
				// extract the root URLs
				URL url = (URL) candidates.nextElement();

				// if it's not a folder, consider it as a root
				if (!url.getFile().endsWith(OsgiResourceUtils.FOLDER_DELIMITER))
					resources.add(new URL(url, "."));
			}
		}
		else {
			// remove leading and trailing / if any
			if (location.startsWith(OsgiResourceUtils.FOLDER_DELIMITER))
				location = location.substring(1);

			if (location.endsWith(OsgiResourceUtils.FOLDER_DELIMITER))
				location = location.substring(0, location.length() - 1);

			// do we have at least on folder or is this just a file
			boolean hasFolder = (location.indexOf(OsgiResourceUtils.FOLDER_DELIMITER) != -1);

			String path = (hasFolder ? location : OsgiResourceUtils.FOLDER_DELIMITER);
			String file = (hasFolder ? null : location);

			// find the file and path
			int separatorIndex = location.lastIndexOf(OsgiResourceUtils.FOLDER_DELIMITER);

			if (separatorIndex > -1 && separatorIndex + 1 < location.length()) {
				// update the path
				path = location.substring(0, separatorIndex);

				// determine file (if there is any)
				if (separatorIndex + 1 < location.length())
					file = location.substring(separatorIndex + 1);
			}

			Enumeration candidates = bundle.findEntries(path, file, false);

			while (candidates != null && candidates.hasMoreElements()) {
				resources.add((URL) candidates.nextElement());
			}
		}

		return (URL[]) resources.toArray(new URL[resources.size()]);
	}

}
