package org.springframework.osgi.extender.support;

import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.beans.BeansException;

/**
 * @author Hal Hildebrand
 *         Date: May 29, 2007
 *         Time: 7:39:17 PM
 */
public interface ServiceDependentOsgiApplicationContext extends ConfigurableOsgiBundleApplicationContext {
    void create(Runnable postAction) throws BeansException;


    void interrupt();


    void complete();


    ContextState getState();


    DependencyListener getListener();
}
