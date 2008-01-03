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
import org.springframework.core.io.Resource;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
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
	protected void assertContextServiceIs(Bundle bundle, boolean alive, long maxWait) {
		Assert.notNull(bundle);

		try {
			waitOnContextCreation(bundle.getSymbolicName(), maxWait / 1000 + 1);
			if (!alive)
				fail("appCtx should have NOT been published for bundle "
						+ OsgiStringUtils.nullSafeNameAndSymName(bundle));
		}
		catch (RuntimeException timeout) {
			if (alive)
				fail("appCtx should have been published for bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle));
		}
	}

	protected Bundle installBundle(String bundleId) throws Exception {
		// locate bundle
		Resource bundleLocation = locateBundle(bundleId);
		assertTrue("bundle " + bundleId + " could not be found", bundleLocation.exists());

		return bundleContext.installBundle(bundleLocation.getURL().toString());
	}
}
