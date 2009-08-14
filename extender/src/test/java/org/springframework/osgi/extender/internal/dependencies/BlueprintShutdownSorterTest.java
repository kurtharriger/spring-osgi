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
package org.springframework.osgi.extender.internal.dependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.springframework.osgi.extender.internal.DependencyMockBundle;
import org.springframework.osgi.extender.internal.dependencies.shutdown.ShutdownSorter;

/**
 * @author Costin Leau
 */
public class BlueprintShutdownSorterTest extends TestCase {

	// see tck-1.dot
	public void testCase1() throws Exception {
		DependencyMockBundle a = new DependencyMockBundle("A");
		DependencyMockBundle b = new DependencyMockBundle("B");
		DependencyMockBundle c = new DependencyMockBundle("C");
		DependencyMockBundle d = new DependencyMockBundle("D");
		DependencyMockBundle e = new DependencyMockBundle("E");

		b.setDependentOn(c);
		d.setDependentOn(e);
		e.setDependentOn(d);

		List<Bundle> order = getOrder(a, b, c, d, e);
		System.out.println("Shutdown order is " + order);
		assertOrder(new Bundle[] { c, a, b, e, d }, order);
	}

	// similar to tck 2 but with D publishes a service with a lower ranking and
	// needs to be destroyed first
	public void testCase2() throws Exception {
		DependencyMockBundle a = new DependencyMockBundle("A");
		DependencyMockBundle b = new DependencyMockBundle("B");
		DependencyMockBundle c = new DependencyMockBundle("C");
		DependencyMockBundle d = new DependencyMockBundle("D");
		DependencyMockBundle e = new DependencyMockBundle("E");

		b.setDependentOn(c);
		d.setDependentOn(e, -13, 12);
		e.setDependentOn(d, 0, 14);

		List<Bundle> order = getOrder(a, b, c, d, e);
		System.out.println("Shutdown order is " + order);
		assertOrder(new Bundle[] { c, a, b, d, e }, order);
	}

	private void assertOrder(Bundle[] expected, List<Bundle> ordered) {
		assertTrue("shutdown order is incorrect", Arrays.equals(expected, ordered.toArray()));
	}

	private List<Bundle> getOrder(Bundle... bundles) {
		List<Bundle> list = new ArrayList<Bundle>(bundles.length);
		list.addAll(Arrays.asList(bundles));
		List<Bundle> result = new ArrayList<Bundle>();

		while (!list.isEmpty()) {
			result.addAll(ShutdownSorter.getBundles(list));
			for (Bundle bundle : result) {
				try {
					bundle.stop();
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		}
		return result;
	}
}
