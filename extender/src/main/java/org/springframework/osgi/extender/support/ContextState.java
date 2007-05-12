/**
 * @author Hal Hildebrand
 * Date: May 9, 2007
 * Time: 3:44:11 PM
 */
package org.springframework.osgi.extender.support;

public class ContextState {
    public static final ContextState INITIALIZED = new ContextState("INITIALIZED");
    public static final ContextState RESOLVING_DEPENDENCIES = new ContextState("RESOLVING_DEPENDENCIES");
    public static final ContextState DEPENDENCIES_RESOLVED = new ContextState("DEPENDENCIES_RESOLVED");
    public static final ContextState CREATED = new ContextState("CREATED");
    public static final ContextState INTERRUPTED = new ContextState("INTERRUPTED");
    public static final ContextState CLOSED = new ContextState("CLOSED");

    private final String myName; // for debug only


    private ContextState(String name) {
        myName = name;
    }


    public String toString() {
        return myName;
    }
}
