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

package org.springframework.osgi.iandt.r41.lazy.ns;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.io.Resource;
import org.springframework.osgi.iandt.r41.lazy.BaseR41IntegrationTest;
import org.springframework.osgi.util.OsgiBundleUtils;

/**
 * Integration test that checks if lazy namespace providers are lazily analyzed.
 * 
 * @author Costin Leau
 */
public class LazyNamespaceProviderTest extends BaseR41IntegrationTest {

	/*
	 * Install the namespace provider lazily. 
	 */
	@Override
	protected void postProcessBundleContext(BundleContext context) throws Exception {
		super.postProcessBundleContext(context);
		Resource resource = locateBundle("org.springframework.osgi.iandt, ns.own.consumer," + getSpringDMVersion());
		Bundle bundle = context.installBundle(resource.getDescription(), resource.getInputStream());
		startBundleLazy(bundle);
		assertTrue("the lazy bundle has been initialized", OsgiBundleUtils.isBundleLazyActivated(bundle));
	}

	// check that the namespace is being picked up through the test configuration file
	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/osgi/iandt/r41/lazy/ns/context.xml" };
	}

	public void testApplicationContextWasProperlyStarted() throws Exception {
		assertNotNull(applicationContext);
		assertNotNull(applicationContext.getBean("nsDate"));
		assertNotNull(applicationContext.getBean("nsBean"));
	}
}
