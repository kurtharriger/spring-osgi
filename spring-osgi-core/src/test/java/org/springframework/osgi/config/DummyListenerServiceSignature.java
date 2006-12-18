package org.springframework.osgi.config;

/**
 * @author Hal Hildebrand
 *         Date: Nov 13, 2006
 *         Time: 12:35:01 PM
 */
public class DummyListenerServiceSignature {
    static int BIND_CALLS = 0;
    static int UNBIND_CALLS = 0;


    public void register(String serviceBeanName, Cloneable service) {
        BIND_CALLS++;
    }


    public void deregister(String serviceBeanName, Cloneable service) {
        UNBIND_CALLS++;
    }
}
