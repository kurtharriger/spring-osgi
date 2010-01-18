/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.osgi.iandt.io;

import java.io.InputStream;
import java.util.Arrays;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.osgi.io.OsgiBundleResourceLoader;
import org.springframework.osgi.io.OsgiBundleResourcePatternResolver;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.util.ObjectUtils;

/**
 * Integration test based on the bug report OSGI-799 regarding discovery of imported packages from the exporting bundle
 * w/o considering their bundle classpath.
 * 
 * The test will install two bundle, one with a custom classpath exporting a package and the other importing that
 * particular package.
 * 
 * @author Costin Leau
 */
public class OSGI799Test extends BaseIoTest {

	private static boolean customBundlesInstalled = false;
	private static final String EXPORT_BND = "org.springframework.bundle.osgi.io.test.osgi799.exp";
	private static final String IMPORT_BND = "org.springframework.bundle.osgi.io.test.osgi799.imp";

	/**
	 * No dependencies installed (we'll do this manually for this test).
	 */
	protected String[] getTestBundlesNames() {
		return new String[0];
	}

	protected String getManifestLocation() {
		return null;
	}

	protected void preProcessBundleContext(BundleContext platformBundleContext) throws Exception {
		super.preProcessBundleContext(platformBundleContext);
		if (!customBundlesInstalled) {
			logger.info("Installing custom bundles...");
			InputStream stream = getClass().getResourceAsStream("/osgi-799-exp.jar");
			assertNotNull(stream);
			Bundle bundle = platformBundleContext.installBundle("osgi-799-exp", stream);
			bundle.start();

			stream = getClass().getResourceAsStream("/osgi-799-imp.jar");
			bundle = platformBundleContext.installBundle("osgi-799-imp", stream);
			bundle.start();

			customBundlesInstalled = true;
		}
	}

	protected String[] getBundleContentPattern() {
		return (String[]) ObjectUtils.addObjectToArray(super.getBundleContentPattern(), "bundleclasspath/**/*");
	}

	/**
	 * Resolves the pattern resolved for the exporting bundle using a custom classpath.
	 * 
	 * @return
	 */
	protected ResourcePatternResolver getExporterPatternLoader() {
		Bundle bundle = OsgiBundleUtils.findBundleBySymbolicName(bundleContext, EXPORT_BND);
		ResourceLoader loader = new OsgiBundleResourceLoader(bundle);
		return new OsgiBundleResourcePatternResolver(loader);
	}

	protected ResourcePatternResolver getImporterPatternLoader() {
		Bundle bundle = OsgiBundleUtils.findBundleBySymbolicName(bundleContext, IMPORT_BND);
		ResourceLoader loader = new OsgiBundleResourceLoader(bundle);
		return new OsgiBundleResourcePatternResolver(loader);
	}

	public void testExportedCustomCP() throws Exception {
		ResourcePatternResolver resolver = getExporterPatternLoader();
		Resource[] resources = resolver.getResources("classpath:/some/**/*.res");
		System.out.println(ObjectUtils.nullSafeToString(resources));
		assertEquals(3, resources.length);
	}

	public void testImportedCustomCP() throws Exception {
		ResourcePatternResolver resolver = getImporterPatternLoader();
		Resource[] resources = resolver.getResources("classpath:some/**/*.res");
		System.out.println(ObjectUtils.nullSafeToString((resources));
		assertEquals(3, resources.length);
	}
	
	public void testExportedCustomFoldersCP() throws Exception {
		ResourcePatternResolver resolver = getExporterPatternLoader();
		Resource[] resources = resolver.getResources("classpath:/**/path/**/*");
		System.out.println(ObjectUtils.nullSafeToString((resources));
		assertEquals(8, resources.length);
	}
	
	public void testImporterCustomFoldersCP() throws Exception {
		ResourcePatternResolver resolver = getImporterPatternLoader();
		Resource[] resources = resolver.getResources("classpath:/**/path/**/*");
		System.out.println(ObjectUtils.nullSafeToString((resources));
		assertEquals(5, resources.length);
	}

	public void testExportedCustomPatternFoldersCP() throws Exception {
		ResourcePatternResolver resolver = getExporterPatternLoader();
		Resource[] resources = resolver.getResources("classpath:/**/p?th/**/*");
		System.out.println(ObjectUtils.nullSafeToString((resources));
		assertEquals(8, resources.length);
	}
	
	public void testImporterCustomPatternFoldersCP() throws Exception {
		ResourcePatternResolver resolver = getImporterPatternLoader();
		Resource[] resources = resolver.getResources("classpath:/**/p?th/**/*");
		System.out.println(ObjectUtils.nullSafeToString((resources));
		assertEquals(5, resources.length);
	}
}