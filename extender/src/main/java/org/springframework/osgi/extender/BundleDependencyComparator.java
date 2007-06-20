package org.springframework.osgi.extender;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
                return 1;
            }
        } else if (bundle2 == null) {
            // Sort nulls first
            return -1;
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
            return 1;
        } else if (b2Lower && !b1Lower) {
            return -1;
        }

        // Doesn't matter, sort consistently on classname
        return bundle1.getSymbolicName().compareTo(bundle2.getSymbolicName());
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
                    if (a.equals(referingBundles[j]) || references(a, referingBundles[j], seen)) {
                        return true;
                    }
                }
            }
        }
        return false;

    }
}