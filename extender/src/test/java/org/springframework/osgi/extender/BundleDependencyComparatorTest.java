package org.springframework.osgi.extender;

import junit.framework.TestCase;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
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
        assertSame(A, bundles[0]);
        assertSame(B, bundles[1]);
        assertSame(C, bundles[2]);
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
        assertSame(A, bundles[0]);
        assertSame(B, bundles[1]);
        assertSame(C, bundles[2]);
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

        A.setUsingBundles(new Bundle[]{});
        B.setUsingBundles(new Bundle[]{A});
        C.setUsingBundles(new Bundle[]{B});
        D.setUsingBundles(new Bundle[]{C});
        E.setUsingBundles(new Bundle[]{D});

        Bundle[] bundles = new Bundle[]{C, E, A};
        Arrays.sort(bundles, new BundleDependencyComparator());
        assertSame(A, bundles[0]);
        assertSame(C, bundles[1]);
        assertSame(E, bundles[2]);
    }


    public void testForrest() throws Exception {
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


    public static class TestBundle extends MockBundle {
        protected Bundle[] usingBundles;
        protected String tag;


        public TestBundle(String tag) {
            this.tag = tag;
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
