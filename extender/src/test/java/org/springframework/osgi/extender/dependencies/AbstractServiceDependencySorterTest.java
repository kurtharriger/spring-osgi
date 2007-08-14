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
package org.springframework.osgi.extender.dependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.springframework.osgi.extender.DependencyMockBundle;
import org.springframework.osgi.extender.dependencies.shutdown.ServiceDependencySorter;
import org.springframework.util.ObjectUtils;

/**
 * Base testing suite for ordering bundles based on service dependencies.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractServiceDependencySorterTest extends TestCase {

	protected ServiceDependencySorter sorter;

	private int count = 1;

	protected void setUp() throws Exception {
		sorter = createSorter();
	}

	protected abstract ServiceDependencySorter createSorter();

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		sorter = null;
	}

	// A -> B -> C
	public void testSimpleTree() {
		DependencyMockBundle a = new DependencyMockBundle("A");
		DependencyMockBundle b = new DependencyMockBundle("B");
		DependencyMockBundle c = new DependencyMockBundle("C");

		a.setDependentOn(b);
		b.setDependentOn(c);

		testDependencyTreeWithShuffle(new Bundle[] { c, b, a }, new Bundle[] { a, b, c });
	}

	// A -> B, C, D
	// B -> C, E
	// C -> E
	// D -> B
	public void testMediumTree() {
		DependencyMockBundle a = new DependencyMockBundle("A");
		DependencyMockBundle b = new DependencyMockBundle("B");
		DependencyMockBundle c = new DependencyMockBundle("C");
		DependencyMockBundle d = new DependencyMockBundle("D");
		DependencyMockBundle e = new DependencyMockBundle("E");

		a.setDependentOn(new Bundle[] { d, c, b });
		b.setDependentOn(new Bundle[] { e, c });
		c.setDependentOn(e);
		d.setDependentOn(b);

		testDependencyTreeWithShuffle(new Bundle[] { e, c, b, d, a }, new Bundle[] { a, b, c, d, e });
	}

	// A -> B
	// B -> A
	public void testSimpleCircularTree() {
		DependencyMockBundle a = new DependencyMockBundle("A");
		DependencyMockBundle b = new DependencyMockBundle("B");

		b.setDependentOn(a);
		a.setDependentOn(b);

		Bundle[] expectedVer1 = new Bundle[] { a, b };
		Bundle[] expectedVer2 = new Bundle[] { b, a };
		assertTrue(Arrays.equals(expectedVer2, sorter.computeServiceDependencyGraph(expectedVer1)));
		assertTrue(Arrays.equals(expectedVer1, sorter.computeServiceDependencyGraph(expectedVer2)));
	}

	// A -> B, C
	// B -> C, D
	// C -> D
	// D -> A
	public void testMediumCircularCycle() {
		DependencyMockBundle a = new DependencyMockBundle("A");
		DependencyMockBundle b = new DependencyMockBundle("B");
		DependencyMockBundle c = new DependencyMockBundle("C");
		DependencyMockBundle d = new DependencyMockBundle("D");

		a.setDependentOn(new Bundle[] { b, c });
		b.setDependentOn(new Bundle[] { c, d });
		c.setDependentOn(d);
		d.setDependentOn(a);

		testDependencyTree(new Bundle[] { d, c, b, a }, new Bundle[] { a, b, c, d });
	}

	// A -> B, C, D
	// B -> C
	// D -> B, E
	// E -> F, G
	// F -> G
	// H -> G
	// I -> H, J

	// depending on the order there are multiple shutdown orders
	public void testLargeTree() {
		DependencyMockBundle a = new DependencyMockBundle("A");
		DependencyMockBundle b = new DependencyMockBundle("B");
		DependencyMockBundle c = new DependencyMockBundle("C");
		DependencyMockBundle d = new DependencyMockBundle("D");
		DependencyMockBundle e = new DependencyMockBundle("E");
		DependencyMockBundle f = new DependencyMockBundle("F");
		DependencyMockBundle g = new DependencyMockBundle("G");
		DependencyMockBundle h = new DependencyMockBundle("H");
		DependencyMockBundle i = new DependencyMockBundle("I");
		DependencyMockBundle j = new DependencyMockBundle("J");

		a.setDependentOn(new Bundle[] { b, c, d });
		b.setDependentOn(c);
		d.setDependentOn(new Bundle[] { b, e });
		e.setDependentOn(new Bundle[] { f, g });
		f.setDependentOn(g);
		h.setDependentOn(g);
		i.setDependentOn(new Bundle[] { h, j });

		testDependencyTree(new Bundle[] { c, b, g, f, e, d, a, h, j, i }, new Bundle[] { a, b, c, d, e, f, g, h, i, j });
		testDependencyTree(new Bundle[] { c, b, g, f, e, d, h, j, i, a }, new Bundle[] { b, d, e, f, g, h, c, i, j, a });
		testDependencyTree(new Bundle[] { g, f, e, h, j, i, c, b, d, a }, new Bundle[] { e, i, c, h, d, a, b, g, j, f });
	}

	// A -> B, D
	// B -> C, E
	// C -> D
	// D -> B, C
	// E -> C
	public void testComplexCyclicTree() {
		DependencyMockBundle a = new DependencyMockBundle("A");
		DependencyMockBundle b = new DependencyMockBundle("B");
		DependencyMockBundle c = new DependencyMockBundle("C");
		DependencyMockBundle d = new DependencyMockBundle("D");
		DependencyMockBundle e = new DependencyMockBundle("E");

		a.setDependentOn(new Bundle[] { b, d });
		b.setDependentOn(new Bundle[] { c, e });
		c.setDependentOn(d);
		d.setDependentOn(new Bundle[] { b, c });
		e.setDependentOn(c);

		testDependencyTree(new Bundle[] { d, c, e, b, a }, new Bundle[] { a, b, c, d, e });
		testDependencyTree(new Bundle[] { c, e, b, d, a }, new Bundle[] { d, c, e, b, a });
		testDependencyTree(new Bundle[] { b, d, c, e, a }, new Bundle[] { e, d, c, a, b });
	}

	// A -> B,D
	// B -> C, E
	// C
	// D -> B, C
	// E -> C
	public void testComplexTree() {
		DependencyMockBundle a = new DependencyMockBundle("A");
		DependencyMockBundle b = new DependencyMockBundle("B");
		DependencyMockBundle c = new DependencyMockBundle("C");
		DependencyMockBundle d = new DependencyMockBundle("D");
		DependencyMockBundle e = new DependencyMockBundle("E");

		a.setDependentOn(new Bundle[] { b, d });
		b.setDependentOn(new Bundle[] { c, e });
		d.setDependentOn(new Bundle[] { b, c });
		e.setDependentOn(new Bundle[] { c });

		testDependencyTreeWithShuffle(new Bundle[] { c, e, b, d, a }, new Bundle[] { a, b, c, d, e });
	}

	/**
	 * Test the resulting tree after shuffling the input bundles several times.
	 * 
	 * @param expected
	 * @param bundles
	 * @return
	 */
	protected void testDependencyTree(Bundle[] expected, Bundle[] bundles) {
		Bundle[] tree = sorter.computeServiceDependencyGraph(bundles);
		assertTrue("array [" + ObjectUtils.nullSafeToString(tree) + "] does not match ["
				+ ObjectUtils.nullSafeToString(expected) + "] for input [" + ObjectUtils.nullSafeToString(bundles)
				+ "]", Arrays.equals(expected, tree));

	}

	protected void testDependencyTreeWithShuffle(Bundle[] expected, Bundle[] bundles) {
		List input = new ArrayList(bundles.length);

		for (int i = 0; i < bundles.length; i++) {
			input.add(bundles[i]);
		}

		// shuffle based on the number of elements
		for (int i = 0; i < bundles.length; i++) {
			testDependencyTree(expected, (Bundle[]) input.toArray(new Bundle[bundles.length]));
			Collections.shuffle(input);
		}

		count += bundles.length;
	}

	public int countTestCases() {
		return count;
	}

}
