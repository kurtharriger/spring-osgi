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
package org.springframework.osgi.extender.dependencies.shutdown;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.springframework.util.ObjectUtils;

/**
 * Recursive implementation for sorting the service dependency tree. Given an
 * unsorted list of bundles, it will pick the first node and first determine the
 * bundles which depend on the node and then, the ones on which the node depends
 * on.
 * 
 * The algorithm travers each node/bundle (and its dependencies) only once.
 * 
 * <strong>Note</strong> This class is thread-safe.
 * 
 * @author Costin Leau
 * 
 */
public class RecursiveServiceDependencySorter implements ServiceDependencySorter {

	private static final Bundle[] EMPTY_BUNDLE_ARRAY = new Bundle[0];

	public Bundle[] computeServiceDependencyGraph(Bundle[] bundles) {
		if (ObjectUtils.isEmpty(bundles))
			return EMPTY_BUNDLE_ARRAY;

		// the dependency list
		List dependencyList = new ArrayList(bundles.length);

		// nodes 'seen' or in-process
		Set seen = new LinkedHashSet(bundles.length);

		// pick up a node
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];

			// if the bundle is 'new', start the process
			if (!seen.contains(bundle)) {
				process(bundle, seen, dependencyList);
			}
		}

		return (Bundle[]) dependencyList.toArray(new Bundle[dependencyList.size()]);
	}

	/**
	 * Recursive method for sorting out the bundles.
	 * 
	 * @param current
	 * @param bundles
	 * @param dependencyList
	 * @param seen
	 */
	private void process(Bundle current, Set seen, List dependencyList) {

		// mark bundle (if it's already marked, return)
		if (!seen.add(current))
			return;

		// find the bundles which depend on this bundle (the bundles
		// which have to be stopped before the current bundle)

		Bundle[] dependentOn = getBundlesWhichDependOn(current);

		for (int i = 0; i < dependentOn.length; i++) {
			Bundle before = dependentOn[i];

			// if the bundle is not filter out and hasn't been processed
			if (filter(before) && !seen.contains(before)) {
				// note: the 'seen' set check is done to avoid useless method
				// invocations that would just return

				// call method recursively
				process(before, seen, dependencyList);
			}
		}

		dependencyList.add(current);

		// find the bundles on which this bundle depends on (thus, the bundles
		// which have to be stopped after this current bundle)
		Bundle[] dependsOn = getBundlesOnWhichDepends(current);

		for (int i = 0; i < dependsOn.length; i++) {
			Bundle after = dependsOn[i];
			if (filter(after) && !seen.contains(after)) {
				process(after, seen, dependencyList);
			}
		}
	}

	/**
	 * Get the bundles on which the given bundle depends on. Works only on
	 * direct connections.
	 * 
	 * @param bundle
	 * @return
	 */
	private Bundle[] getBundlesOnWhichDepends(Bundle bundle) {
		ServiceReference[] refs = bundle.getServicesInUse();

		if (refs == null)
			return EMPTY_BUNDLE_ARRAY;

		Set bundles = new LinkedHashSet(refs.length);

		for (int i = 0; i < refs.length; i++) {
			bundles.add(refs[i].getBundle());
		}

		return (Bundle[]) bundles.toArray(new Bundle[bundles.size()]);
	}

	/**
	 * Get the bundles which depend on the given argument. Works only on direct
	 * connections.
	 * 
	 * @param bundle
	 * @return
	 */
	private Bundle[] getBundlesWhichDependOn(Bundle bundle) {

		ServiceReference[] refs = bundle.getRegisteredServices();

		if (refs == null)
			return EMPTY_BUNDLE_ARRAY;

		Set bundles = new LinkedHashSet(refs.length);

		// get services that use this service
		for (int i = 0; i < refs.length; i++) {
			Bundle[] usingBundles = refs[i].getUsingBundles();

			for (int j = 0; j < usingBundles.length; j++) {
				bundles.add(usingBundles[i]);
			}
		}

		return (Bundle[]) bundles.toArray(new Bundle[bundles.size()]);
	}

	/**
	 * Filtering method for bundles, used mainly when discovering bundles
	 * outside the given bundle array.
	 * 
	 * @param bundle
	 * @return true if the bundle should be included or false otherwise
	 */
	protected boolean filter(Bundle bundle) {
		return true;
	}
}
