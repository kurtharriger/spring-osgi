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
package org.springframework.osgi.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.framework.Bundle;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Resource implementation for OSGi environments. Lazy evaluation of the
 * resource will be used.
 * 
 * Understands the "bundle:" resource prefix for explicit loading of resources
 * from the bundle. When the bundle prefix is used the target resource must be
 * contained within the bundle (or attached fragments), the classpath is not
 * searched.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 * 
 */
public class OsgiBundleResource extends AbstractResource {

	public static final String BUNDLE_URL_PREFIX = "bundle:";
	private static final char PREFIX_SEPARATOR = ':';
	private static final String ABSOLUTE_PATH_PREFIX = "/";

	private final Bundle bundle;
	private final String path;
	
	public OsgiBundleResource(Bundle bundle, String path) {
		Assert.notNull(bundle, "Bundle must not be null");
		this.bundle = bundle;

		// check path
		Assert.notNull(path, "Path must not be null");

		this.path = StringUtils.cleanPath(path);
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
	 * This implementation returns a URL for the underlying bundle resource.
	 * 
	 * @see org.osgi.framework.Bundle#getEntry(String)
	 * @see org.osgi.framework.Bundle#getResource(String)
	 */
	public URL getURL() throws IOException {
		URL url = null;

		if (this.path.startsWith(BUNDLE_URL_PREFIX)) {
			url = getResourceFromBundle(this.path.substring(BUNDLE_URL_PREFIX.length()));
		}
		else if (this.path.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
			url = getResourceFromBundleClasspath(this.path.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length()));
		}

		// Costin: default fallback - take from resource classpath
		// TODO: should it matter if the path is absolute or not?
		else {
			url = getResourceFromBundleClasspath(this.path);
		}

		if (url == null) {
			throw new FileNotFoundException(getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * Resolves a resource from *this bundle only*. Only the bundle and its
	 * attached fragments are searched for the given resource.
	 * 
	 * @param bundleRelativePath
	 * @return a URL to the returned resource or null if none is found
	 * 
	 * @see org.osgi.framework.Bundle#getEntry(String)
	 */
	protected URL getResourceFromBundle(String bundleRelativePath) {
		//TODO: Felix workaround - should be removed when issue is better
		// understood. Felix returns URLs that look like "/0/<path to file>",
		// but then doesn't like the prefix "/0" when you pass the same thing
		// back into getEntry, so we trim it off here...
		bundleRelativePath = felixHack(bundleRelativePath);
		return bundle.getEntry(bundleRelativePath);
	}

	private String felixHack(String bundlePath) {
		if (bundlePath.startsWith("/0")) {
			return bundlePath.substring(2);
		}
		else {
			return bundlePath;
		}
	}
	
	/**
	 * Resolves a resource from the bundle's classpath. This will find resources
	 * in this bundle and also in imported packages from other bundles.
	 * 
	 * @param bundleRelativePath
	 * @return a URL to the returned resource or null if none is found
	 * 
	 * @see org.osgi.framework.Bundle#getResource(String)
	 */
	protected URL getResourceFromBundleClasspath(String bundleRelativePath) {
		//TODO: Felix workaround - should be removed when issue is better
		// understood. Felix returns URLs that look like "/0/<path to file>",
		// but then doesn't like the prefix "/0" when you pass the same thing
		// back into getEntry, so we trim it off here...
		bundleRelativePath = felixHack(bundleRelativePath);		
		return bundle.getResource(bundleRelativePath);
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
	 *      String)
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
		return "OSGi bundle resource [" + this.path + "]";
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
}
