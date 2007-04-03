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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 
 * OSGi-aware subclass of PathMatchingResourcePatternResolver, able to find
 * matching resources inside an OSGi bundle root directory via OSGi API's
 * <code>Bundle.getEntryPaths</code> and <code>Bundle.getResources</code>
 * Falls back to the superclass' file system checking for other resources.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiBundleResourcePatternResolver extends PathMatchingResourcePatternResolver {

	public OsgiBundleResourcePatternResolver(Bundle bundle) {
		super(new OsgiBundleResourceLoader(bundle));
	}

	public OsgiBundleResourcePatternResolver(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	public Resource[] getResources(String locationPattern) throws IOException {
		Assert.notNull(locationPattern, "Location pattern must not be null");
		// Remove classpath* and pattern with classpath: support for now
		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
			throw new IllegalArgumentException(CLASSPATH_ALL_URL_PREFIX + " not supported");
		}
		else {
			if (getPathMatcher().isPattern(locationPattern)) {
				String prefix = getPrefix(locationPattern);

				// no prefix is treated just like bundle:
				if (!StringUtils.hasText(prefix))
					prefix = OsgiBundleResource.BUNDLE_URL_PREFIX;

				if (!OsgiBundleResource.BUNDLE_URL_PREFIX.equals(prefix))
					throw new IllegalArgumentException("patterns allowed only with "
							+ OsgiBundleResource.BUNDLE_URL_PREFIX);
				// a file pattern for bundle
				// return
				// findPathMatchingResources(locationPattern.substring(OsgiBundleResource.BUNDLE_URL_PREFIX.length()));
				return findPathMatchingResources(locationPattern);
			}
			else {
				// a single resource with the given name
				return new Resource[] { getResourceLoader().getResource(locationPattern) };
			}
		}
	}

	/**
	 * Adds support for patterns starting with bundle:
	 * 
	 * @see OsgiBundleResource#BUNDLE_URL_PREFIX
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver#getResources(java.lang.String)
	 */

	protected Set doFindPathMatchingFileResources(Resource rootDirResource, String subPattern) throws IOException {

		if (rootDirResource instanceof OsgiBundleResource) {
			OsgiBundleResource bundleResource = (OsgiBundleResource) rootDirResource;
			// use the Url to get the path (bundle.getPath() can contain bundle:
			// or classpath: prefix)

			// don't call this since it fails on Felix
			//String cleanPath = bundleResource.getURL().getPath();
			String rootPath = bundleResource.getPath();
			// strip prefix
			int index = rootPath.indexOf(":");
			String cleanPath = (index > -1 ? rootPath.substring(index + 1) : rootPath);
			String fullPattern = cleanPath + subPattern;
			Set result = CollectionFactory.createLinkedSetIfPossible(16);
			doRetrieveMatchingBundleEntries(bundleResource.getBundle(), fullPattern, cleanPath, result);
			return result;
		}

		return super.doFindPathMatchingFileResources(rootDirResource, subPattern);
	}

	/**
	 * Seach each level inside the bundle for entries (resources which are
	 * retrieved w/o using the bundle classloader). The method queries each
	 * folder for all entries and applies matching afterwards. The Bundle API
	 * contains a recursive method for applying matching but the pattern is
	 * different from the Ant-style for example (does not recognize ** for
	 * folders nor ?). The method while not extremely efficient, allows custom
	 * pattern matchers to be applied without any knowledge of the underlying
	 * OSGi matching implementation.
	 * 
	 * @param bundle the bundle to do the lookup
	 * @param fullPattern matching pattern
	 * @param dir directory inside the bundle
	 * @param result set of results (used to concatenate matching sub dirs).
	 * @throws IOException
	 */
	protected void doRetrieveMatchingBundleEntries(Bundle bundle, String fullPattern, String dir, Set result)
			throws IOException {

		// get only the resources from current folder (use null instead of * to
		// make it work over mocks also)
		Enumeration candidates = bundle.findEntries(dir, null, false);
		if (candidates != null) {
			boolean dirDepthNotFixed = (fullPattern.indexOf("**") != -1);
			while (candidates.hasMoreElements()) {
				URL currURL = (URL) candidates.nextElement();
				String currPath = currURL.getPath();
				if (!currPath.startsWith(dir)) {
					// Returned resource path does not start with relative
					// directory:
					// assuming absolute path returned -> strip absolute path.
					int dirIndex = currPath.indexOf(dir);
					if (dirIndex != -1) {
						currPath = currPath.substring(dirIndex);
					}
				}
				if (currPath.endsWith("/")
						&& (dirDepthNotFixed || StringUtils.countOccurrencesOf(currPath, "/") < StringUtils.countOccurrencesOf(
							fullPattern, "/"))) {
					// Search subdirectories recursively: we manually get the
					// folders on only one level
					doRetrieveMatchingBundleEntries(bundle, fullPattern, currPath, result);
				}
				if (getPathMatcher().match(fullPattern, currPath)) {
					result.add(new OsgiBundleResource(bundle, currPath));
				}
			}
		}
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
