package org.springframework.osgi.test.reference.proxy;

import org.springframework.osgi.test.simpleservice.MyService;

/**
 * @author Hal Hildebrand
 *         Date: Nov 25, 2006
 *         Time: 12:50:20 PM
 */
public class ServiceReferer {
    public static MyService serviceReference;

    public void setReference(MyService reference) {
        serviceReference = reference;
    }
}
