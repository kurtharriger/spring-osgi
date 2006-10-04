package org.springframework.osgi.service;

import junit.framework.TestCase;
import org.springframework.beans.factory.BeanFactory;
import org.easymock.MockControl;
import org.easymock.internal.AlwaysMatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Properties;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
public class ServiceExporterTests extends TestCase {

    private ServiceExporter exporter = new ServiceExporter();
    private BeanFactory beanFactory;
    private MockControl beanFactoryControl;
    private BundleContext bundleContext;
    private MockControl bundleContextControl;
    private MockControl mockServiceRegistrationControl;

    protected void setUp() throws Exception {
        this.beanFactoryControl = MockControl.createControl(BeanFactory.class);
        this.beanFactory = (BeanFactory) this.beanFactoryControl.getMock();
        this.bundleContextControl = MockControl.createControl(BundleContext.class);
        this.bundleContext = (BundleContext) this.bundleContextControl.getMock();
    }

    public void testAfterPropertiesSetNoBeans() throws Exception {
        this.exporter.setBeanFactory(this.beanFactory);
        this.exporter.setBundleContext(this.bundleContext);
        this.bundleContextControl.replay();
        this.beanFactoryControl.replay();
        this.exporter.afterPropertiesSet();
        this.bundleContextControl.verify();
        this.beanFactoryControl.verify();
    }

    public void testAfterPropertiesSetNoBundleContext() throws Exception {
        this.exporter.setBeanFactory(this.beanFactory);
        try {
            this.exporter.afterPropertiesSet();
            fail("Expecting IllegalArgumentException");
        }
        catch(IllegalArgumentException ex) {
            assertEquals("Required property bundleContext has not been set",
                    ex.getMessage());
        }
    }

    public void testAfterPropertiesSetNoResolver() throws Exception {
        this.exporter.setBeanFactory(this.beanFactory);
        this.exporter.setBundleContext(this.bundleContext);
        this.exporter.setResolver(null);
        try {
            this.exporter.afterPropertiesSet();
            fail("Expecting IllegalArgumentException");
        }
        catch(IllegalArgumentException ex) {
            assertEquals("Required property resolver was set to a null value",
                    ex.getMessage());
        }
    }

    public void testAfterPropertiesSetNoBeanFactory() throws Exception {
        try {
            this.exporter.afterPropertiesSet();
            fail("Expecting IllegalArgumentException");
        }
        catch(IllegalArgumentException ex) {
            assertEquals("Required property beanFactory has not been set",
                    ex.getMessage());
        }
    }

    public void testPublish() throws Exception {
        this.exporter.setBeanFactory(this.beanFactory);
        this.exporter.setBundleContext(this.bundleContext);
        MockControl mc = MockControl.createControl(OsgiServicePropertiesResolver.class);
        OsgiServicePropertiesResolver resolver = (OsgiServicePropertiesResolver) mc.getMock();
        this.exporter.setResolver(resolver);
        this.exporter.setExportedBean("thisBean");

        // set expectations on afterProperties
        this.beanFactory.getBean("thisBean");
        Object thisBean = new Object();
        this.beanFactoryControl.setReturnValue(thisBean);

        resolver.getServiceProperties("thisBean");
        mc.setReturnValue(new Properties());

        this.bundleContext.registerService((String)null, null, null);
        this.bundleContextControl.setMatcher(new AlwaysMatcher());
        this.bundleContextControl.setReturnValue(getServiceRegistration());
        this.bundleContext.registerService((String)null, null, null);
        this.bundleContextControl.setReturnValue(getServiceRegistration());

        this.bundleContextControl.replay();
        this.beanFactoryControl.replay();
        mc.replay();

        // do the work
        this.exporter.afterPropertiesSet();

        // verify
        this.bundleContextControl.verify();
        this.beanFactoryControl.verify();
        mc.verify();
    }

    public void testDestroy() throws Exception {
        testPublish();
        this.mockServiceRegistrationControl.replay();
        this.exporter.destroy();
        this.mockServiceRegistrationControl.verify();
    }

    private ServiceRegistration getServiceRegistration() {
        this.mockServiceRegistrationControl = MockControl.createControl(ServiceRegistration.class);
        ServiceRegistration ret = (ServiceRegistration) this.mockServiceRegistrationControl.getMock();
        ret.unregister(); // for destroy test..
        return ret;
    }

}
