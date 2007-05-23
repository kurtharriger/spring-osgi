package org.springframework.osgi.iandt.annotation.proxy;

import org.springframework.osgi.iandt.simpleservice.MyService;
import org.springframework.osgi.annotation.ServiceReference;

/**
 * @author Andy Piper
 */
public class ServiceReferer {
    public static MyService serviceReference;

    @ServiceReference
    public void setServiceReference(MyService reference) {
        serviceReference = reference;
    }
}
