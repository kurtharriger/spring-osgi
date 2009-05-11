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

package org.springframework.osgi.iandt.r41.lazy.export;

import java.awt.Shape;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.iandt.r41.lazy.BaseR41IntegrationTest;
import org.springframework.osgi.test.platform.OsgiPlatform;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;

/**
 * @author Costin Leau
 */
public class ExportNoImportsTest extends BaseR41IntegrationTest {

	private static final String SYM_NAME = "org.springframework.osgi.iandt.lazy.export.no.import";


	@Override
	protected String[] lazyBundles() {
		return new String[] { "org.springframework.osgi.iandt, lazy.companion.bundle," + getSpringDMVersion(),
			"org.springframework.osgi.iandt, lazy.export.no.import.bundle," + getSpringDMVersion() };
	}

	private ServiceReference getActivationMarker() {
		return OsgiServiceReferenceUtils.getServiceReference(bundleContext, Shape.class.getName(), "(lazy.marker=true)");
	}

	private void checkLazyServices() throws Exception {
		assertNotNull(getServiceReference(OsgiFilterUtils.unifyFilter("org.springframework.osgi.iandt.lazy.SomeClass",
			null)));
		assertNotNull(getServiceReference(OsgiFilterUtils.unifyFilter(new String[] { "java.io.Serializable",
			"java.util.Map" }, null)));
	}

	public void testContextStartedOnlyIfBundleActivated() throws Exception {

		Bundle bnd = OsgiBundleUtils.findBundleBySymbolicName(bundleContext, SYM_NAME);

		checkThatBundleIsStillLazy(bnd);
		// check services
		checkLazyServices();

		assertTrue("Bundle has been activated", OsgiBundleUtils.isBundleLazyActivated(bnd));

		checkThatBundleIsStillLazy(bnd);
		// trigger class loading
		try {
			bnd.loadClass("@");
		}
		catch (Exception expected) {
		}

		assertFalse("Bundle has not been actived", OsgiBundleUtils.isBundleLazyActivated(bnd));
		waitOnContextCreation(SYM_NAME);
		assertNotNull("marker has not been published", getActivationMarker());
	}

	private void checkThatBundleIsStillLazy(Bundle bundle) {
		// test if bundle is still lazy
		assertTrue("Bundle has been activated", OsgiBundleUtils.isBundleLazyActivated(bundle));
		assertNull("marker has been published", getActivationMarker());
	}
}