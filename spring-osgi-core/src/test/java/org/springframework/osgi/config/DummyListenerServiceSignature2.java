package org.springframework.osgi.config;

/**
 * @author Hal Hildebrand
 *         Date: Nov 13, 2006
 *         Time: 12:35:01 PM
 */
public class DummyListenerServiceSignature2 {
    static int BIND_CALLS = 0;
    static int UNBIND_CALLS = 0;


    public void register(Cloneable service) {
        BIND_CALLS++;
    }


    public void deregister(Cloneable service) {
        UNBIND_CALLS++;
    }
}
