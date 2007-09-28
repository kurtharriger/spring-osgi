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

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Area;

import org.osgi.framework.Bundle;
import org.springframework.osgi.util.OsgiBundleUtils;

/**
 * Integration test for Sync Wait but this time, by checking the waiting by
 * satisfing the dependency through this test.
 * 
 * @author Costin Leau
 * 
 */
public class SyncWaitWithoutDependencyTest extends BehaviorBaseTest {

	public void testBehaviour() throws Exception {

		String bundleId = "org.springframework.osgi, org.springframework.osgi.iandt.sync-wait-bundle,"
				+ getSpringOsgiVersion();

		// locate bundle
		String tailBundleId = "org.springframework.osgi, org.springframework.osgi.iandt.sync-tail-bundle,"
				+ getSpringOsgiVersion();

		// start bundle first (no dependency)
		Bundle bundle = installBundle(bundleId);
		bundle.start();

		assertTrue("bundle " + bundle + "hasn't been fully started", OsgiBundleUtils.isBundleActive(bundle));
		// start bundle first (no dependency)
		Bundle tailBundle = installBundle(tailBundleId);
		tailBundle.start();

		assertTrue("bundle " + tailBundle + "hasn't been fully started", OsgiBundleUtils.isBundleActive(tailBundle));

		// wait for the listener to get the bundles and wait for timeout
		Thread.sleep(500);

		// check appCtx hasn't been published
		assertContextServiceIs(bundle, false);
		// check the dependency ctx
		assertContextServiceIs(tailBundle, false);
		// and service
		assertNull(getBundleContext().getServiceReference(Area.class.getName()));

		// put a Shape service up (a polygon but not an area)
		getBundleContext().registerService(Shape.class.getName(), new Polygon(), null);

		// let bundle pick this up
		Thread.sleep(500);

		// check appCtx hasn't been published
		assertContextServiceIs(bundle, true);
		// check the dependency ctx
		assertContextServiceIs(tailBundle, true);
		// and service
		assertNotNull(getBundleContext().getServiceReference(Area.class.getName()));

	}
}
