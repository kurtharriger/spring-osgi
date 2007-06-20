package org.springframework.osgi.extender;

import junit.framework.TestCase;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Constants;

import org.springframework.osgi.mock.MockBundle;
import org.springframework.osgi.mock.MockServiceReference;

import java.util.Arrays;

/**
 * @author Hal Hildebrand
 *         Date: Jun 19, 2007
 *         Time: 9:12:02 PM
 */
public class BundleDependencyComparatorTest extends TestCase {

    public void testSimpleLinear() throws Exception {
        TestBundle A = new TestBundle("A");
        TestBundle B = new TestBundle("B");
        TestBundle C = new TestBundle("C");

        A.setUsingBundles(new Bundle[]{});
        B.setUsingBundles(new Bundle[]{A});
        C.setUsingBundles(new Bundle[]{B});

        Bundle[] bundles = new Bundle[]{C, A, B};
        Arrays.sort(bundles, new BundleDependencyComparator());
        assertSame(C, bundles[0]);
        assertSame(B, bundles[1]);
        assertSame(A, bundles[2]);
    }


    public void testReverseLinear() throws Exception {
        TestBundle A = new TestBundle("A");
        TestBundle B = new TestBundle("B");
        TestBundle C = new TestBundle("C");

        A.setUsingBundles(new Bundle[]{});
        B.setUsingBundles(new Bundle[]{A});
        C.setUsingBundles(new Bundle[]{B});

        Bundle[] bundles = new Bundle[]{C, B, A};
        Arrays.sort(bundles, new BundleDependencyComparator());
        assertSame(C, bundles[0]);
        assertSame(B, bundles[1]);
        assertSame(A, bundles[2]);
    }


    public void testInOrderLinear() throws Exception {
        TestBundle A = new TestBundle("A");
        TestBundle B = new TestBundle("B");
        TestBundle C = new TestBundle("C");

        A.setUsingBundles(new Bundle[]{});
        B.setUsingBundles(new Bundle[]{A});
        C.setUsingBundles(new Bundle[]{B});

        Bundle[] bundles = new Bundle[]{A, B, C};
        Arrays.sort(bundles, new BundleDependencyComparator());
        assertSame(C, bundles[0]);
        assertSame(B, bundles[1]);
        assertSame(A, bundles[2]);
    }


    public void testMissingMiddle() throws Exception {
        TestBundle A = new TestBundle("A");
        TestBundle B = new TestBundle("B");
        TestBundle C = new TestBundle("C");
        TestBundle D = new TestBundle("D");
        TestBundle E = new TestBundle("E");

        A.setUsingBundles(new Bundle[]{});
        B.setUsingBundles(new Bundle[]{A});
        C.setUsingBundles(new Bundle[]{B});
        D.setUsingBundles(new Bundle[]{C});
        E.setUsingBundles(new Bundle[]{D});

        Bundle[] bundles = new Bundle[]{C, E, A};
        Arrays.sort(bundles, new BundleDependencyComparator());
        assertSame(E, bundles[0]);
        assertSame(C, bundles[1]);
        assertSame(A, bundles[2]);
    }

    public void testCircularReferenceId() throws Exception {
        TestBundle A = new TestBundle("A", 0, 0);
        TestBundle B = new TestBundle("B", 0, 1);
        TestBundle C = new TestBundle("C", 0, 2);
        TestBundle D = new TestBundle("D", 0, 3);
        TestBundle E = new TestBundle("E", 0, 4);

        A.setUsingBundles(new Bundle[]{E});
        B.setUsingBundles(new Bundle[]{A});
        C.setUsingBundles(new Bundle[]{B});
        D.setUsingBundles(new Bundle[]{C});
        E.setUsingBundles(new Bundle[]{D});

        Bundle[] bundles = new Bundle[]{E, D, C, B, A};
        Arrays.sort(bundles, new BundleDependencyComparator());
        assertSame(E, bundles[0]);
        assertSame(D, bundles[1]);
        assertSame(C, bundles[2]);
        assertSame(B, bundles[3]);
        assertSame(A, bundles[4]);
    }

    public void testCircularReferenceReference() throws Exception {
        TestBundle A = new TestBundle("A", 4, 0);
        TestBundle B = new TestBundle("B", 3, 1);
        TestBundle C = new TestBundle("C", 2, 2);
        TestBundle D = new TestBundle("D", 1, 3);
        TestBundle E = new TestBundle("E", 0, 4);

        A.setUsingBundles(new Bundle[]{E});
        B.setUsingBundles(new Bundle[]{A});
        C.setUsingBundles(new Bundle[]{B});
        D.setUsingBundles(new Bundle[]{C});
        E.setUsingBundles(new Bundle[]{D});

        Bundle[] bundles = new Bundle[]{E, D, C, B, A};
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

        A.setUsingBundles(new Bundle[]{});
        B.setUsingBundles(new Bundle[]{A});
        C.setUsingBundles(new Bundle[]{B});
        D.setUsingBundles(new Bundle[]{B});
        E.setUsingBundles(new Bundle[]{D});

        F.setUsingBundles(new Bundle[]{});
        G.setUsingBundles(new Bundle[]{F});
        H.setUsingBundles(new Bundle[]{F});

        I.setUsingBundles(new Bundle[]{});
        J.setUsingBundles(new Bundle[]{I});

        Bundle[] bundles = new Bundle[]{F, D, J, B, E, A, H, I, G, C};
        Arrays.sort(bundles, new BundleDependencyComparator());
        assertSame(C, bundles[0]);
        assertSame(E, bundles[1]);
        assertSame(D, bundles[2]);
        assertSame(B, bundles[3]);
        assertSame(A, bundles[4]);
        assertSame(G, bundles[5]);
        assertSame(H, bundles[6]);
        assertSame(F, bundles[7]);
        assertSame(J, bundles[8]);
        assertSame(I, bundles[9]);
    }


    public static class TestBundle extends MockBundle {
        protected Bundle[] usingBundles;
        protected String tag;
        protected int rank;
        protected int id;

        public TestBundle(String tag, int rank, int id) {
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
            return new ServiceReference[]{
                    new MockServiceReference() {
                        public Bundle[] getUsingBundles() {
                            return usingBundles;
                        }
                        public Object getProperty(String p) {
                            if (p.equals(Constants.SERVICE_RANKING)) {
                                return new Integer(rank);
                            }
                            else if (p.equals(Constants.SERVICE_ID)) {
                                return new Long(id);
                            }
                            return null;
                        }
                    }
            };
        }


        public String toString() {
            return tag;
        }


        public String getSymbolicName() {
            return tag;
        }
    }
}
