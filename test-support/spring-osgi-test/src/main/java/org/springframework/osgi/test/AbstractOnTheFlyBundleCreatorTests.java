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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.objectweb.asm.ClassReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.osgi.internal.test.storage.MemoryStorage;
import org.springframework.osgi.internal.test.util.DependencyVisitor;
import org.springframework.osgi.internal.test.util.JarCreator;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
public abstract class AbstractOnTheFlyBundleCreatorTests extends AbstractDependencyManagerTests {

	protected JarCreator jarCreator;

	public AbstractOnTheFlyBundleCreatorTests() {
		initializeJarCreator();
	}

	public AbstractOnTheFlyBundleCreatorTests(String testName) {
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
	 * 
	 * Subclasses should override this method to enhance the returned Manifest.
	 * 
	 * @return Manifest used for this test suite.
	 * 
	 * @see #createDefaultManifest()
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
	 * Create the default manifest in case none if found on the disk. By
	 * default, the imports are synthetised based on the test class bytecode.
	 * 
	 * @return
	 */
	protected Manifest createDefaultManifest() {
		Manifest manifest = new Manifest();
		Attributes attrs = manifest.getMainAttributes();

		// manifest versions
		attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		attrs.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");

		String description = getName() + "-" + getClass().getName();
		// name/description
		attrs.putValue(Constants.BUNDLE_NAME, "TestBundle-" + description);
		attrs.putValue(Constants.BUNDLE_SYMBOLICNAME, "TestBundle-" + description);
		attrs.putValue(Constants.BUNDLE_DESCRIPTION, "on-the-fly test bundle");

		// activator
		attrs.putValue(Constants.BUNDLE_ACTIVATOR, JUnitTestActivator.class.getName());

		// add Import-Package entry
		addImportPackage(manifest);

		if (logger.isDebugEnabled())
			logger.debug("created manifest:" + manifest.getMainAttributes().entrySet());
		return manifest;
	}

	private void addImportPackage(Manifest manifest) {
		String[] rawImports = determineImports(getClass());

		boolean trace = logger.isTraceEnabled();

		if (trace)
			logger.trace("Discovered raw imports " + ObjectUtils.nullSafeToString(rawImports));

		Collection imports = eliminateSpecialPackages(rawImports);

		if (trace)
			logger.trace("Filtered imports are " + imports);

		manifest.getMainAttributes().putValue(Constants.IMPORT_PACKAGE,
			StringUtils.collectionToCommaDelimitedString(imports));
	}

	/**
	 * Eliminate 'special' packages (java.*, test framework internal and the
	 * class declaring package)
	 * 
	 * @param rawImports
	 * @return
	 */
	private Collection eliminateSpecialPackages(String[] rawImports) {
		String currentPckg = ClassUtils.classPackageAsResourcePath(getClass()).replace('/', '.');

		Set filteredImports = new LinkedHashSet(rawImports.length);

		for (int i = 0; i < rawImports.length; i++) {
			String pckg = rawImports[i];

			if (!(pckg.startsWith("java.") || pckg.startsWith("org.springframework.osgi.internal.test") || pckg.equals(currentPckg)))
				filteredImports.add(pckg);
		}

		return filteredImports;
	}

	/**
	 * Determine imports by walking a class hierarchy until the current package
	 * is found.
	 * 
	 * @return
	 */
	private String[] determineImports(Class clazz) {
		Assert.notNull(clazz, "a not-null class is required");
		String endPackage = ClassUtils.classPackageAsResourcePath(AbstractOnTheFlyBundleCreatorTests.class).replace(
			'/', '.');

		Set cumulatedPackages = new LinkedHashSet();

		String clazzPackage;

		do {
			cumulatedPackages.addAll(determineImportsForClass(clazz));
			clazzPackage = ClassUtils.classPackageAsResourcePath(clazz).replace('/', '.');
			clazz = clazz.getSuperclass();
		} while (!endPackage.equals(clazzPackage));

		String[] packages = (String[]) cumulatedPackages.toArray(new String[cumulatedPackages.size()]);
		// sort the array
		Arrays.sort(packages);

		for (int i = 0; i < packages.length; i++) {
			packages[i] = packages[i].replace('/', '.');
		}

		return packages;
	}

	private Set determineImportsForClass(Class clazz) {
		Assert.notNull(clazz, "a not-null class is required");
		DependencyVisitor visitor = new DependencyVisitor();
		ClassReader reader;
		try {
			reader = new ClassReader(clazz.getResourceAsStream(ClassUtils.getClassFileName(clazz)));
		}
		catch (Exception ex) {
			throw (RuntimeException) new IllegalArgumentException("cannot read class " + clazz).initCause(ex);
		}
		reader.accept(visitor, false);

		return visitor.getPackages();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.AbstractOsgiTests#postProcessBundleContext(org.osgi.framework.BundleContext)
	 */
	protected void postProcessBundleContext(BundleContext context) throws Exception {
		logger.debug("post processing: creating test bundle");

		// add the content pattern
		jarCreator.setContentPattern(getBundleContentPattern());

		// create the actual jar
		Resource jar = jarCreator.createJar(getManifest());

		try {
			installAndStartBundle(context, jar);
		}
		catch (Exception e) {
			IllegalStateException ise = new IllegalStateException(
					"Unable to dynamically start generated bundle for Unit test");
			ise.initCause(e);
			throw ise;
		}

		// now do the delegation
		super.postProcessBundleContext(context);
	}

	private void installAndStartBundle(BundleContext context, Resource resource) throws Exception {
		// install & start
		Bundle bundle = context.installBundle("[onTheFly-test-bundle]" + ClassUtils.getShortName(getClass()) + "["
				+ hashCode() + "]", resource.getInputStream());

		String bundleString = OsgiStringUtils.nullSafeNameAndSymName(bundle);
		boolean debug = logger.isDebugEnabled();

		if (debug)
			logger.debug("test bundle [" + bundleString + "] succesfully installed");
		bundle.start();
		if (debug)
			logger.debug("test bundle [" + bundleString + "] succesfully started");
	}

}
