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
package org.springframework.osgi.test.support;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.test.OsgiJUnitTest;
import org.springframework.osgi.test.TestRunnerService;

/**
 * Default activator for Spring/OSGi test support. This class can be seen as the 'server-side' of the framework,
 * which register the OsgiJUnitTest executor.
 * 
 * @author Costin Leau
 * 
 */
public class Activator implements BundleActivator {

	private ServiceRegistration registration;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		registration = context.registerService(TestRunnerService.class.getName(), new OsgiJUnitService(), new Hashtable());
		
		// add also the bundle id so that AbstractOsgiTest can determine its BundleContext when used in an environment
		// where the system bundle is treated as a special case (such as mBeddedServer).
		System.getProperties().put(OsgiJUnitTest.OSGI_TEST_BUNDLE_ID, new Long(context.getBundle().getBundleId()));

	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		// unregister the service even though the framework should do this automatically
		if (registration != null)
			registration.unregister();
	}

}
