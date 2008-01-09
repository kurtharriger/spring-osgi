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

package org.springframework.osgi.iandt.compliance.service;

import java.awt.Polygon;
import java.awt.Shape;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.util.OsgiFilterUtils;

/**
 * @author Costin Leau
 * 
 */
public class ServiceAvailableDuringUnregistrationTest extends AbstractConfigurableBundleCreatorTests {

	private Shape service;


	public void testServiceAliveDuringUnregistration() throws Exception {
		service = new Polygon();

		ServiceRegistration reg = bundleContext.registerService(Shape.class.getName(), service, null);

		String filter = OsgiFilterUtils.unifyFilter(Shape.class, null);

		ServiceListener listener = new ServiceListener() {

			public void serviceChanged(ServiceEvent event) {
				if (ServiceEvent.UNREGISTERING == event.getType()) {
					ServiceReference ref = event.getServiceReference();
					Object aliveService = bundleContext.getService(ref);
					assertNotNull("services not available during unregistration", aliveService);
					assertSame(service, aliveService);
				}
			}
		};

		try {
			bundleContext.addServiceListener(listener, filter);
			reg.unregister();
		}
		finally {
			bundleContext.removeServiceListener(listener);
		}
	}

	protected boolean isDisabledInThisEnvironment(String testMethodName) {
		return (createPlatform().toString().startsWith("Knopflerfish"));
	}

}
