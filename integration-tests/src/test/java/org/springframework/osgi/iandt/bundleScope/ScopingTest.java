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
package org.springframework.osgi.iandt.bundleScope;

import org.osgi.framework.ServiceReference;
import org.springframework.osgi.iandt.scope.common.ScopeTestService;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.util.ObjectUtils;

/**
 * Integration tests for 'bundle' scoped beans.
 * 
 * @author Costin Leau
 * 
 */
public class ScopingTest extends AbstractConfigurableBundleCreatorTests {

	protected String[] getBundles() {
		return new String[] {
				localMavenArtifact("org.springframework.osgi", "cglib-nodep.osgi", "2.1.3-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.iandt.scoped.bundle.common",
					getSpringOsgiVersion()),
				localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.iandt.scoped.bundle.a",
					getSpringOsgiVersion()),
				localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.iandt.scoped.bundle.b",
					getSpringOsgiVersion()) };
	}

	protected String getManifestLocation() {
		return "org/springframework/osgi/iandt/bundleScope/ScopingTest.MF";
		// return null;
	}

	public void testEnvironmentValidity() throws Exception {
		assertNotNull(getServiceA());
		assertNotNull(getServiceB());
	}

	public void testServiceAScopeForCurrentBundle() throws Exception {
		ScopeTestService serviceAcopy1 = getServiceA();
		ScopeTestService serviceAcopy2 = getServiceA();

		assertEquals("different bean instances given for the same bundle", serviceAcopy1, serviceAcopy2);
	}

	public void testServiceAScopeForBundleA() throws Exception {
		ScopeTestService serviceAInBundleA = (ScopeTestService) org.springframework.osgi.iandt.scope.a.BeanReference.BEAN;

		assertFalse("same bean instance used for different bundles",
			serviceAInBundleA.equals(getServiceA().getServiceIdentity()));
	}

	public void testServiceAScopeForBundleB() throws Exception {
		ScopeTestService serviceAInBundleB = (ScopeTestService) org.springframework.osgi.iandt.scope.b.BeanReference.BEAN;

		assertFalse("same bean instance used for different bundles",
			serviceAInBundleB.equals(getServiceA().getServiceIdentity()));
	}

	public void testServiceAScopeForBundleAAndBundleB() throws Exception {
		ScopeTestService serviceAInBundleA = (ScopeTestService) org.springframework.osgi.iandt.scope.a.BeanReference.BEAN;
		ScopeTestService serviceAInBundleB = (ScopeTestService) org.springframework.osgi.iandt.scope.b.BeanReference.BEAN;

		assertFalse("same bean instance used for different bundles", serviceAInBundleA.getServiceIdentity().equals(
			serviceAInBundleB.getServiceIdentity()));
	}

	protected ScopeTestService getServiceA() throws Exception {
		return getService("a");
	}

	protected ScopeTestService getServiceB() throws Exception {
		return getService("b");
	}

	protected ScopeTestService getService(String bundleName) {
		ServiceReference ref = OsgiServiceReferenceUtils.getServiceReference(getBundleContext(),
			ScopeTestService.class.getName(), "(Bundle-SymbolicName=org.springframework.osgi.iandt.scope." + bundleName
					+ ")");
		if (ref == null) {
			String filter = OsgiFilterUtils.unifyFilter(ScopeTestService.class, null);
			System.out.println(ObjectUtils.nullSafeToString(OsgiServiceReferenceUtils.getServiceReferences(getBundleContext(), filter)));
			throw new IllegalStateException("cannot find service with owning bundle " + bundleName);
		}
		return (ScopeTestService) getBundleContext().getService(ref);

	}
}
