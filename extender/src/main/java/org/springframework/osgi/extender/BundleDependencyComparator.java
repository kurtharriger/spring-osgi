package org.springframework.osgi.extender;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Constants;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

/**
 * @author Hal Hildebrand
 *         Date: Jun 19, 2007
 *         Time: 8:56:12 PM
 */
public class BundleDependencyComparator implements Comparator {

    public int compare(Object a, Object b) {
        Bundle bundle1 = (Bundle) a;
        Bundle bundle2 = (Bundle) b;
        if (bundle1 == null) {
            if (bundle2 == null) {
                return 0;
            } else {
                // Sort nulls first
                return -1;
            }
        } else if (bundle2 == null) {
            // Sort nulls first
            return 1;
        }

        // At this point, we know that bundle1 and bundle2 are not null
        if (bundle1.equals(bundle2)) {
            return 0;
        }

        // At this point, bundle1 and bundle2 are not null and not equal, here we
        // compare them to see which is "higher" in the dependency graph
        boolean b1Lower = references(bundle2, bundle1);
        boolean b2Lower = references(bundle1, bundle2);

        if (b1Lower && !b2Lower) {
            return -1;
        } else if (b2Lower && !b1Lower) {
            return 1;
        }
        // Deal with circular references and unrelated bundles.
        return compareUsingServiceRankingAndId(bundle1, bundle2);
    }


    /**
     * Answer whether Bundle b is referenced by Bundle a
     */
    protected boolean references(Bundle a, Bundle b) {
        return references(a, b, new HashSet());
    }


    /**
     * Answer whether Bundle b is transitively referenced by Bundle a
     */
    protected boolean references(Bundle a, Bundle b, Set seen) {
        if (seen.contains(b)) {
            return false;
        }
        seen.add(b);
        ServiceReference[] services = b.getRegisteredServices();
        if (services == null) {
            return false;
        }
        for (int i = 0; i < services.length; i++) {
            Bundle[] referingBundles = services[i].getUsingBundles();
            if (referingBundles != null) {
                for (int j = 0; j < referingBundles.length; j++) {
                    if (a.equals(referingBundles[j])) {
                        return true;
                    }
                    else if (references(a, referingBundles[j], seen)) {
                        return true;
                    }
                }
            }
        }
        return false;

    }

    private static class RankingComparator implements Comparator {
            public int compare(Object o1, Object o2) {
                Integer i1 = ((Integer)((ServiceReference)o1).getProperty(Constants.SERVICE_RANKING));
                Integer i2 = ((Integer)((ServiceReference)o2).getProperty(Constants.SERVICE_RANKING));
                if (i1 == null && i2 == null) return 0;
                else if (i1==null) return 1;
                else if (i2==null) return -1;
                return i2.intValue()-i1.intValue();
            }
        }

    private static class IdComparator implements Comparator {
            public int compare(Object o1, Object o2) {
                int i1 = ((Long)((ServiceReference)o1).getProperty(Constants.SERVICE_ID)).intValue();
                int i2 = ((Long)((ServiceReference)o2).getProperty(Constants.SERVICE_ID)).intValue();
                return i1-i2;
            }
        }

    /**
     * Answer whether Bundle a is higher or lower depending on the ranking and id of
     * its exported services. This is used as a tie-breaker for circular references.
     */
    protected int compareUsingServiceRankingAndId(Bundle a, Bundle b) {
        ServiceReference[] aservices = a.getRegisteredServices();
        Arrays.sort(aservices, new RankingComparator());
        ServiceReference[] bservices = b.getRegisteredServices();
        Arrays.sort(bservices, new RankingComparator());
        Integer i0 = ((Integer)(aservices[0].getProperty(Constants.SERVICE_RANKING)));
        Integer i1 = ((Integer)(bservices[0].getProperty(Constants.SERVICE_RANKING)));

        if (i0 != null && i1 == null) {
            return 1;
        }
        else if (i1 != null && i0 == null) {
            return -1;
        }
        else if (i0 != i1 && i0.intValue() != i1.intValue()) {
            return i0.intValue()-i1.intValue();
        }
        Arrays.sort(aservices, new IdComparator());
        Arrays.sort(bservices, new IdComparator());
        int k0 = ((Long)(aservices[0].getProperty(Constants.SERVICE_ID))).intValue();
        int k1 = ((Long)(bservices[0].getProperty(Constants.SERVICE_ID))).intValue();
		if (k1 != k0) {
			return k1-k0;
		}
		// Doesn't matter, sort consistently on classname
		return a.getSymbolicName().compareTo(b.getSymbolicName());
    }
}