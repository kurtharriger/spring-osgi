package org.springframework.osgi.test.service.listener;

import org.springframework.osgi.test.simpleservice.MyService;

/**
 * @author Hal Hildebrand
 *         Date: Mar 29, 2007
 *         Time: 2:50:07 AM
 */

/**
 * Exists purely as a means to force the creation of the service reference so that the listener
 * can be created - we need to add the "lazy-init" flag to <osgi:reference>
 */
public class Anchor {
    private MyService reference;


    public void setReference(MyService reference) {
        this.reference = reference;
    }
}
