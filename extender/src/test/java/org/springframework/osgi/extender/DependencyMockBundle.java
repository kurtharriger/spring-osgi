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
package org.springframework.osgi.extender;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.mock.MockBundle;
import org.springframework.osgi.mock.MockServiceReference;

/**
 * Mock bundle useful for testing service dependencies.
 * 
 * @author Costin Leau
 * 
 */
public class DependencyMockBundle extends MockBundle {

	// bundles which depend on the current one
	protected Bundle[] dependentOn;

	// bundles on which the current bundle depends on
	protected Bundle[] dependsOn;

	private ServiceReference[] inUseServices;

	private ServiceReference[] registeredServices;

	public DependencyMockBundle() {
		super();
	}

	public DependencyMockBundle(BundleContext context) {
		super(context);
	}

	public DependencyMockBundle(Dictionary headers) {
		super(headers);
	}

	public DependencyMockBundle(String location, Dictionary headers, BundleContext context) {
		super(location, headers, context);
	}

	public DependencyMockBundle(String location) {
		super(location);
	}

	/**
	 * Create one service reference returning the using bundle.
	 * 
	 * @param dependent
	 */
	public void setDependentOn(final Bundle[] dependent) {
		this.dependentOn = dependent;

		// initialize registered services
		registeredServices = new ServiceReference[dependent.length];
		for (int i = 0; i < registeredServices.length; i++) {
			
			registeredServices[i] = new MockServiceReference() {

				public Bundle getBundle() {
					return DependencyMockBundle.this;
				}

				public Bundle[] getUsingBundles() {
					return dependent;
				}
			};
		}
	}

	public void setDependentOn(Bundle dependent) {
		setDependentOn(new Bundle[] { dependent });
	}

	protected void setDependsOn(Bundle[] depends) {
		this.dependsOn = depends;

		// initialize InUseServices
		inUseServices = new ServiceReference[depends.length];

		final Bundle[] usingBundles = new Bundle[] { this };

		for (int i = 0; i < dependsOn.length; i++) {
			final Bundle dependencyBundle = dependsOn[i];

			// make connection from the opposite side also
			if (dependencyBundle instanceof DependencyMockBundle) {
				((DependencyMockBundle) dependencyBundle).setDependentOn(this);
			}

			inUseServices[i] = new MockServiceReference() {
				public Bundle getBundle() {
					return dependencyBundle;
				}

				public Bundle[] getUsingBundles() {
					return usingBundles;
				}
			};
		}
	}

	protected void setDependsOn(Bundle depends) {
		setDependsOn(new Bundle[] { depends });
	}

	public ServiceReference[] getRegisteredServices() {
		return registeredServices;
	}

	public ServiceReference[] getServicesInUse() {
		return inUseServices;
	}

}
