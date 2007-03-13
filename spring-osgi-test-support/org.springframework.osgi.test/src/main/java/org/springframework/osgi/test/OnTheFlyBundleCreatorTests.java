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

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.osgi.test.util.JarCreator;
import org.springframework.osgi.test.util.JarUtils;
import org.springframework.osgi.test.util.ManifestUtils;
import org.springframework.osgi.test.util.MemoryStorage;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Enhanced subclass of {@link AbstractDependencyManagerTests} which facilitates
 * OSGi testing by creating at runtime, on the fly, a jar using the indicated
 * manifest and resource patterns (by default all files found under the root
 * path).
 * 
 * <p/> Note that in more complex scenarios, dedicated packaging tools (such as
 * ant scripts or maven2) should be used.
 * 
 * 
 * @author Costin Leau
 * 
 */
public abstract class OnTheFlyBundleCreatorTests extends AbstractDependencyManagerTests {

	private JarCreator jarCreator;

	public OnTheFlyBundleCreatorTests() {
		initializeJarCreator();
	}

	public OnTheFlyBundleCreatorTests(String testName) {
		super(testName);
		initializeJarCreator();
	}

	private void initializeJarCreator() {
		jarCreator = new JarCreator();
		jarCreator.setStorage(new MemoryStorage());
	}

	/**
	 * Patterns for identifying the resources added to the jar. The patterns are
	 * added to the root path when performing the search.
	 * 
	 * @return the patterns
	 */
	protected String[] getBundleContentPattern() {
		return JarCreator.DEFAULT_CONTENT_PATTERN;
	}

	/**
	 * Return the location (in Spring resource style) of the manifest location
	 * to be used. If the manifest is created programatically, return a null
	 * string and use {@link #getManifest()} and
	 * {@link #createDefaultManifest()}.
	 * 
	 * @return the manifest location
	 */
	protected String getManifestLocation() {
		return null;
	}

	/**
	 * Return the current test bundle manifest. The method tries to read the
	 * manifest from the given location; in case the location is null, will
	 * create a <code>Manifest</code> object containing default entries.
	 * 
	 * Subclasses should override this method to enhance the returned Manifest.
	 * 
	 * @return Manifest used for this test suite.
	 * 
	 * @throws Exception
	 */
	protected Manifest getManifest() {
		String manifestLocation = getManifestLocation();
		if (StringUtils.hasText(manifestLocation)) {
			DefaultResourceLoader loader = new DefaultResourceLoader();
			Resource res = loader.getResource(manifestLocation);
			try {
				return new Manifest(res.getInputStream());
			}
			catch (IOException ex) {
				throw new RuntimeException("cannot retrieve manifest from " + res);
			}
		}

		else {
			return createDefaultManifest();
		}
	}

	/**
	 * Create the default manifest in case none if found on the disk.
	 * 
	 * @return
	 */
	protected Manifest createDefaultManifest() {
		Manifest manifest = new Manifest();
		Attributes attrs = manifest.getMainAttributes();

		// manifest versions
		attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		attrs.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");

		// name/description
		attrs.putValue(Constants.BUNDLE_NAME, "Test Bundle[" + this + "]");
		attrs.putValue(Constants.BUNDLE_SYMBOLICNAME, "Test Bundle[" + this + "]");
		attrs.putValue(Constants.BUNDLE_DESCRIPTION, "on-the-fly test bundle");

		// activator
		attrs.putValue(Constants.BUNDLE_ACTIVATOR, JUnitTestActivator.class.getName());

		// add Import-Package entry
		addImportPackage(manifest);

		if (log.isDebugEnabled())
			log.debug("created manifest:" + manifest.getMainAttributes().entrySet());
		return manifest;
	}

	private void addImportPackage(Manifest manifest) {
		StringBuffer importPackages = new StringBuffer();

		Set packages = new LinkedHashSet();
		packages.add("org.osgi.framework;specification-version=\"1.3.0\"");

		// mandatory bundles
		String[] mandatoryBundlesExports = ManifestUtils.determineImportPackages(locateBundles(getMandatoryBundles()));

		CollectionUtils.mergeArrayIntoCollection(mandatoryBundlesExports, packages);

		if (log.isDebugEnabled())
			log
					.debug("Import package from mandatory bundles: "
							+ ObjectUtils.nullSafeToString(mandatoryBundlesExports));

		// importPackages.append(mandatoryBundlesExports);
		// importPackages.append(StringUtils.arrayToCommaDelimitedString(getMandatoryPackageBundleImport()));

		// optional/user-defined packages
		String[] optionalBundlesExports = ManifestUtils.determineImportPackages(locateBundles(getBundles()));

		if (log.isDebugEnabled())
			log.debug("Import package from optional bundles: " + ObjectUtils.nullSafeToString(optionalBundlesExports));

		// importPackages.append(optionalBundlesExports);

		CollectionUtils.mergeArrayIntoCollection(optionalBundlesExports, packages);

		manifest.getMainAttributes().putValue(Constants.IMPORT_PACKAGE,
				StringUtils.collectionToCommaDelimitedString(packages));
	}

	private String[] getMandatoryPackageBundleImport() {
		return new String[] { "junit.framework", "org.osgi.framework;specification-version=\"1.3.0\"",
				"org.springframework.core.io", "org.springframework.util", "org.springframework.osgi.test",
				"org.springframework.osgi.test.platform", "org.springframework.osgi.context.support;version=",
				"org.springframework.beans", "org.springframework.beans.factory", "org.springframework.context",
				"org.apache.commons.logging" };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.AbstractOsgiTests#postProcessBundleContext(org.osgi.framework.BundleContext)
	 */
	protected void postProcessBundleContext(BundleContext context) throws Exception {
		log.debug("post processing: creating test bundle");

		// create the actual jar
		Resource jar = jarCreator.createJar(getManifest());

		if (log.isTraceEnabled())
			log.trace("created jar:\n" + JarUtils.dumpJarContent(jar));

		installAndStartBundle(context, jar);
	}

	private void installAndStartBundle(BundleContext context, Resource resource) throws Exception {
		// install & start
		Bundle bundle = context.installBundle("[onTheFly-test-bundle]" + ClassUtils.getShortName(getClass()) + "["
				+ hashCode() + "]", resource.getInputStream());

		log.debug("test bundle succesfully installed");
		bundle.start();
		log.debug("test bundle succesfully started");

	}
}
