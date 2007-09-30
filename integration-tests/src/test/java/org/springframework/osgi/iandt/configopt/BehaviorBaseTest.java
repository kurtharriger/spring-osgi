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
package org.springframework.osgi.iandt.configopt;

import org.osgi.framework.Bundle;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.Assert;

/**
 * Base class with utility methods.
 * 
 * @author Costin Leau
 * 
 */
public abstract class BehaviorBaseTest extends AbstractConfigurableBundleCreatorTests {

	/**
	 * Does the given bundle, publish an application context or not?
	 * 
	 * @param alive
	 */
	protected void assertContextServiceIs(Bundle bundle, boolean alive) {
		Assert.notNull(bundle);

		// generate filter
		String query = "(" + ConfigurableOsgiBundleApplicationContext.APPLICATION_CONTEXT_SERVICE_PROPERTY_NAME + "="
				+ bundle.getSymbolicName() + ")";

		// do query
		Object refs = OsgiServiceReferenceUtils.getServiceReference(bundleContext,
			ApplicationContext.class.getName(), query);
		// make sure the appCtx is up
		if (alive)
			assertNotNull("appCtx should have been published for bundle "
					+ OsgiStringUtils.nullSafeNameAndSymName(bundle), refs);
		else
			assertNull("appCtx should not have been published for bundle "
					+ OsgiStringUtils.nullSafeNameAndSymName(bundle), refs);

	}

	protected Bundle installBundle(String bundleId) throws Exception {
		// locate bundle
		Resource bundleLocation = locateBundle(bundleId);
		assertTrue("bundle " + bundleId + " could not be found", bundleLocation.exists());

		return bundleContext.installBundle(bundleLocation.getURL().toString());
	}
}
