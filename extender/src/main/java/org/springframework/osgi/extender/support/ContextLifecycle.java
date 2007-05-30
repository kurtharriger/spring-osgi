/**
 * @author Hal Hildebrand
 * Date: May 30, 2007
 * Time: 3:01:46 PM
 */
package org.springframework.osgi.extender.support;

public class ContextLifecycle {
    public static final ContextLifecycle PENDING = new ContextLifecycle("PENDING");
    public static final ContextLifecycle CREATING = new ContextLifecycle("CREATING");
    public static final ContextLifecycle CREATED = new ContextLifecycle("CREATED");
    public static final ContextLifecycle DESTROYING = new ContextLifecycle("DESTROYING");
    public static final ContextLifecycle DESTROYED = new ContextLifecycle("DESTROYED");

    private final String myName; // for debug only


    private ContextLifecycle(String name) {
        myName = name;
    }


    public String toString() {
        return myName;
    }
}
