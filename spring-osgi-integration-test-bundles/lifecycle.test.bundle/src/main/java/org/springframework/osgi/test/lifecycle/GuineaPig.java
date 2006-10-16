package org.springframework.osgi.test.lifecycle;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;

/**
 * @author Hal Hildebrand
 *         Date: Oct 15, 2006
 *         Time: 5:23:16 PM
 */
public class GuineaPig implements InitializingBean, DisposableBean, BundleContextAware {
    BundleContext bundleContext;
    Listener listener;


    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }


    public void afterPropertiesSet() throws Exception {
        System.setProperty("org.springframework.osgi.test.lifecycle.GuineaPig.startUp", "true");
        listener = new Listener();
        bundleContext.addFrameworkListener(listener);
    }


    public void destroy() throws Exception {
        bundleContext.removeFrameworkListener(listener);
        System.setProperty("org.springframework.osgi.test.lifecycle.GuineaPig.close", "true");
    }


    static class Listener implements FrameworkListener {
        public void frameworkEvent(FrameworkEvent frameworkEvent) {
            System.out.println("Eavesdropping on " + frameworkEvent);
        }
    }
}
