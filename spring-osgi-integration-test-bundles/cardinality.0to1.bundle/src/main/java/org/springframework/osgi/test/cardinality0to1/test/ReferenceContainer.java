package org.springframework.osgi.test.cardinality0to1.test;

import org.springframework.osgi.test.simpleservice2.MyService2;

/**
 * @author Hal Hildebrand
 *         Date: Apr 16, 2007
 *         Time: 3:23:40 PM
 */
public class ReferenceContainer {
    public static MyService2 service;


    public void setSimpleService(MyService2 simpleService) {
        service = simpleService;
    }
}
