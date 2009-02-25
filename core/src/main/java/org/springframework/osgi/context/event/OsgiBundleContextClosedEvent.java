package org.springframework.osgi.context.event;

import org.springframework.context.ApplicationContext;
import org.osgi.framework.Bundle;

/**
 * Event raised when an <tt>ApplicationContext#close()</tt> method executes
 * successfully inside an OSGi.
 *
 * @author Andy Piper
 */
public class OsgiBundleContextClosedEvent extends OsgiBundleApplicationContextEvent {

    /**
     * Constructs a new <code>OsgiBundleContextClosedEvent</code> instance.
     *
     * @param source event source
     * @param bundle associated OSGi bundle
     */
    public OsgiBundleContextClosedEvent(ApplicationContext source, Bundle bundle) {
        super(source, bundle);
    }

}
