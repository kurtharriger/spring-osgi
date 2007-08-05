package org.springframework.osgi.extender;

import junit.framework.TestCase;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.mock.MockBundle;
import org.springframework.osgi.mock.MockServiceReference;

import java.util.Arrays;

/**
 * @author Hal Hildebrand
 * @author Andy Piper
 */
public class BundleDependencyComparatorTest extends TestCase {

	public void testSimpleLinear() throws Exception {
		TestBundle A = new TestBundle("A");
		TestBundle B = new TestBundle("B");
		TestBundle C = new TestBundle("C");

		// Sets dependency A -> B -> C
		A.setUsingBundles(new Bundle[] {});
		B.setUsingBundles(new Bundle[] { A });
		C.setUsingBundles(new Bundle[] { B });

		Bundle[] bundles = new Bundle[] { C, A, B };
		Arrays.sort(bundles, new BundleDependencyComparator());
		assertSame(A, bundles[0]);
		assertSame(B, bundles[1]);
		assertSame(C, bundles[2]);
	}

	public void testReverseLinear() throws Exception {
		TestBundle A = new TestBundle("A");
		TestBundle B = new TestBundle("B");
		TestBundle C = new TestBundle("C");

		// Sets dependency A -> B -> C
		A.setUsingBundles(new Bundle[] {});
		B.setUsingBundles(new Bundle[] { A });
		C.setUsingBundles(new Bundle[] { B });

		Bundle[] bundles = new Bundle[] { C, B, A };
		Arrays.sort(bundles, new BundleDependencyComparator());
		assertSame(A, bundles[0]);
		assertSame(B, bundles[1]);
		assertSame(C, bundles[2]);
	}

	public void testInOrderLinear() throws Exception {
		TestBundle A = new TestBundle("A");
		TestBundle B = new TestBundle("B");
		TestBundle C = new TestBundle("C");

		// Sets dependency A -> B -> C
		A.setUsingBundles(new Bundle[] {});
		B.setUsingBundles(new Bundle[] { A });
		C.setUsingBundles(new Bundle[] { B });

		Bundle[] bundles = new Bundle[] { A, B, C };
		Arrays.sort(bundles, new BundleDependencyComparator());
		assertSame(A, bundles[0]);
		assertSame(B, bundles[1]);
		assertSame(C, bundles[2]);
	}

	public void testMissingMiddle() throws Exception {
		TestBundle A = new TestBundle("A");
		TestBundle B = new TestBundle("B");
		TestBundle C = new TestBundle("C");
		TestBundle D = new TestBundle("D");
		TestBundle E = new TestBundle("E");

		// Sets dependency A -> B -> C -> D -> E
		A.setUsingBundles(new Bundle[] {});
		B.setUsingBundles(new Bundle[] { A });
		C.setUsingBundles(new Bundle[] { B });
		D.setUsingBundles(new Bundle[] { C });
		E.setUsingBundles(new Bundle[] { D });

		Bundle[] bundles = new Bundle[] { C, E, A };
		Arrays.sort(bundles, new BundleDependencyComparator());
		assertSame(A, bundles[0]);
		assertSame(C, bundles[1]);
		assertSame(E, bundles[2]);
	}

	public void testCircularReferenceId() throws Exception {
		TestBundle A = new TestBundle("A", 0, 0);
		TestBundle B = new TestBundle("B", 0, 1);
		TestBundle C = new TestBundle("C", 0, 2);
		TestBundle D = new TestBundle("D", 0, 3);
		TestBundle E = new TestBundle("E", 0, 4);

		// Sets dependency A -> B -> C -> D -> E -> A
		// A has lowest id so gets shutdown last (started first).
		A.setUsingBundles(new Bundle[] { E });
		B.setUsingBundles(new Bundle[] { A });
		C.setUsingBundles(new Bundle[] { B });
		D.setUsingBundles(new Bundle[] { C });
		E.setUsingBundles(new Bundle[] { D });

		Bundle[] bundles = new Bundle[] { E, D, C, B, A };
		Arrays.sort(bundles, new BundleDependencyComparator());
		assertSame(E, bundles[0]);
		assertSame(D, bundles[1]);
		assertSame(C, bundles[2]);
		assertSame(B, bundles[3]);
		assertSame(A, bundles[4]);
	}

	public void testCircularReferenceIdMulti() throws Exception {
		TestBundle A = new TestBundle("A", 0, 0);
		TestBundle B = new TestBundle("B", new int[] { 0, 0 }, new int[] { 4, 1 });
		TestBundle C = new TestBundle("C", 0, 2);

		// Sets dependency A -> B -> C -> A
		// A has lowest id so gets shutdown last (started first).
		A.setUsingBundles(new Bundle[] { C });
		B.setUsingBundles(new Bundle[] { A });
		C.setUsingBundles(new Bundle[] { B });

		Bundle[] bundles = new Bundle[] { C, B, A };
		Arrays.sort(bundles, new BundleDependencyComparator());
		assertSame(C, bundles[0]);
		assertSame(B, bundles[1]);
		assertSame(A, bundles[2]);
	}

	public void testCircularReferenceRankMulti() throws Exception {
		TestBundle A = new TestBundle("A", 0, 0);
		TestBundle B = new TestBundle("B", new int[] { 0, 3 }, new int[] { 0, 0 });
		TestBundle C = new TestBundle("C", 2, 0);

		// Sets dependency A -> B -> C -> A
		// B has highest rank so gets shutdown last (started first).
		A.setUsingBundles(new Bundle[] { C });
		B.setUsingBundles(new Bundle[] { A });
		C.setUsingBundles(new Bundle[] { B });

		Bundle[] bundles = new Bundle[] { C, B, A };
		Arrays.sort(bundles, new BundleDependencyComparator());
		assertSame(A, bundles[0]);
		assertSame(C, bundles[1]);
		assertSame(B, bundles[2]);
	}

	public void testCircularReferenceReference() throws Exception {
		TestBundle A = new TestBundle("A", 4, 0);
		TestBundle B = new TestBundle("B", 3, 1);
		TestBundle C = new TestBundle("C", 2, 2);
		TestBundle D = new TestBundle("D", 1, 3);
		TestBundle E = new TestBundle("E", 0, 4);

		// Sets dependency A -> B -> C -> D -> E -> A
		// A has higher ranking so gets shutd own last
		A.setUsingBundles(new Bundle[] { E });
		B.setUsingBundles(new Bundle[] { A });
		C.setUsingBundles(new Bundle[] { B });
		D.setUsingBundles(new Bundle[] { C });
		E.setUsingBundles(new Bundle[] { D });

		Bundle[] bundles = new Bundle[] { E, D, C, B, A };
		Arrays.sort(bundles, new BundleDependencyComparator());
		assertSame(E, bundles[0]);
		assertSame(D, bundles[1]);
		assertSame(C, bundles[2]);
		assertSame(B, bundles[3]);
		assertSame(A, bundles[4]);
	}

	public void testForest() throws Exception {
		TestBundle A = new TestBundle("A");
		TestBundle B = new TestBundle("B");
		TestBundle C = new TestBundle("C");
		TestBundle D = new TestBundle("D");
		TestBundle E = new TestBundle("E");
		TestBundle F = new TestBundle("F");
		TestBundle G = new TestBundle("G");
		TestBundle H = new TestBundle("H");
		TestBundle I = new TestBundle("I");
		TestBundle J = new TestBundle("J");

		// Sets dependency A -> B -> C B -> D -> E
		A.setUsingBundles(new Bundle[] {});
		B.setUsingBundles(new Bundle[] { A });
		C.setUsingBundles(new Bundle[] { B });
		D.setUsingBundles(new Bundle[] { B });
		E.setUsingBundles(new Bundle[] { D });

		// Sets dependency F -> G F -> H
		F.setUsingBundles(new Bundle[] {});
		G.setUsingBundles(new Bundle[] { F });
		H.setUsingBundles(new Bundle[] { F });

		// Sets dependency I -> J
		I.setUsingBundles(new Bundle[] {});
		J.setUsingBundles(new Bundle[] { I });

		Bundle[] bundles = new Bundle[] { F, D, J, B, E, A, H, I, G, C };
		Arrays.sort(bundles, new BundleDependencyComparator());
		assertSame(A, bundles[0]);
		assertSame(B, bundles[1]);
		assertSame(C, bundles[2]);
		assertSame(D, bundles[3]);
		assertSame(E, bundles[4]);
		assertSame(F, bundles[5]);
		assertSame(G, bundles[6]);
		assertSame(H, bundles[7]);
		assertSame(I, bundles[8]);
		assertSame(J, bundles[9]);
	}

	private static class TestBundle extends MockBundle {
		protected Bundle[] usingBundles;

		protected String tag;

		protected int[] rank;

		protected int[] id;

		public TestBundle(String tag, int rank, int id) {
			this.tag = tag;
			this.rank = new int[] { rank };
			this.id = new int[] { id };
		}

		public TestBundle(String tag, int[] rank, int[] id) {
			this.tag = tag;
			this.rank = rank;
			this.id = id;
		}

		public TestBundle(String tag) {
			this(tag, 0, 0);
		}

		public String getTag() {
			return tag;
		}

		public void setUsingBundles(Bundle[] usingBundles) {
			this.usingBundles = usingBundles;
		}

		public ServiceReference[] getRegisteredServices() {
			ServiceReference[] refs = new ServiceReference[rank.length];
			for (int i = 0; i < rank.length; i++) {
				final int r = i;
				refs[i] = new MockServiceReference() {
					public Bundle[] getUsingBundles() {
						return usingBundles;
					}

					public Object getProperty(String p) {
						if (p.equals(Constants.SERVICE_RANKING)) {
							return new Integer(rank[r]);
						}
						else if (p.equals(Constants.SERVICE_ID)) {
							return new Long(id[r]);
						}
						return null;
					}
				};
			}
			return refs;
		}

		public String toString() {
			return tag;
		}

		public String getSymbolicName() {
			return tag;
		}
	}
}
