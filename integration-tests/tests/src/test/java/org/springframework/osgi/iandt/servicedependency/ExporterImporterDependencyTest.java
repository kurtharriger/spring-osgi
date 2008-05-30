/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.iandt.servicedependency;

import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.iandt.BaseIntegrationTest;
import org.springframework.osgi.iandt.tccl.TCCLService;
import org.springframework.osgi.util.OsgiBundleUtils;

/**
 * Integration test for the dependency between exporters and importers
 * 
 * @author Costin Leau
 * 
 */
public class ExporterImporterDependencyTest extends BaseIntegrationTest {

	private static final String DEP_SYN_NAME = "org.springframework.osgi.iandt.tccl";


	protected String[] getTestBundlesNames() {
		// load the tccl bundle as it exposes a simple service
		return new String[] { "org.springframework.osgi.iandt,tccl," + getSpringDMVersion() };
	}

	protected synchronized String[] getConfigLocations() {
		// trigger loading of TCCLService
		if (TCCLService.class != null) {
			this.notify();
		}

		return new String[] { "org/springframework/osgi/iandt/servicedependency/config.xml" };
	}

	public void testExporterGoesDownAndAUpWithTheDependencyImporter() throws Exception {
		// check map exporter

		ServiceReference ref = bundleContext.getServiceReference(Map.class.getName());
		Map service = (Map) bundleContext.getService(ref);

		assertSame(applicationContext.getBean("map"), service);
		Bundle dependency = getDependencyBundle();
		// stop bundle (and thus the exposed service)
		dependency.stop();
		// check if map is still published
		assertNull("exported should have been unpublished", ref.getBundle());
		// double check the service space
		assertNull("the map service should be unregistered", bundleContext.getServiceReference(Map.class.getName()));

		dependency.start();
		waitOnContextCreation(DEP_SYN_NAME);

		// the old reference remains invalid
		assertNull("the reference should remain invalid", ref.getBundle());
		// but the service should be back again
		assertSame(applicationContext.getBean("map"), service);
	}

	private Object getService(Class type) {
		ServiceReference ref = bundleContext.getServiceReference(type.getName());
		return bundleContext.getService(ref);
	}

	private Bundle getDependencyBundle() {
		return OsgiBundleUtils.findBundleBySymbolicName(bundleContext, DEP_SYN_NAME);
	}
}
