package org.springframework.osgi.iandt.duplicateClass;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.iandt.simpleservice.MyService;

/**
 * @author Andy Piper
 */
public class DuplicateClassTest extends AbstractConfigurableBundleCreatorTests {
    private static final String DEPENDENT_CLASS_NAME = "org.springframework.osgi.iandt.simpleservice.MyService";

    // private static final String SERVICE_2_FILTER = "(service=2)";
    // private static final String SERVICE_3_FILTER = "(service=3)";

    protected String getManifestLocation() {
        return "classpath:org/springframework/osgi/iandt/duplicateClass/DuplicateClassTest.MF";
    }

    protected String[] getBundles() {
        return new String[] {
                localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT") };
    }

    public void testDependencies() throws Exception {
        // waitOnContextCreation("org.springframework.osgi.iandt.simpleservice");

        BundleContext bundleContext = getBundleContext();

        Bundle simpleServiceBundle = bundleContext.installBundle(getLocator().locateArtifact(
            "org.springframework.osgi", "org.springframework.osgi.iandt.simple.service", getSpringOsgiVersion()).getURL().toExternalForm());
        Bundle simpleServiceDuplicateBundle = bundleContext.installBundle(getLocator().locateArtifact(
            "org.springframework.osgi", "org.springframework.osgi.iandt.simple.service.identical", getSpringOsgiVersion()).getURL().toExternalForm());

        assertNotNull("Cannot find the simple service bundle", simpleServiceBundle);
        assertNotNull("Cannot find the simple service duplicate bundle", simpleServiceDuplicateBundle);

        assertNotSame("simple service bundle is in the activated state!", new Integer(Bundle.ACTIVE), new Integer(
                simpleServiceBundle.getState()));

        assertNotSame("simple service 2 bundle is in the activated state!", new Integer(Bundle.ACTIVE), new Integer(
                simpleServiceDuplicateBundle.getState()));

        startDependency(simpleServiceDuplicateBundle);
        startDependency(simpleServiceBundle);

        ServiceReference[] refs = bundleContext.getServiceReferences(DEPENDENT_CLASS_NAME, null);

        assertEquals(2, refs.length);

        MyService service1 = (MyService)bundleContext.getService(refs[0]);
        MyService service2 = (MyService)bundleContext.getService(refs[1]);

        assertNotNull(service1);
        assertNotNull(service2);

        String msg1 = service1.stringValue();
        String msg2 = service2.stringValue();

        String jmsg = "Bond.  James Bond.";
        String cmsg = "Connery.  Sean Connery #1";
        System.out.println(msg1);
        System.out.println(msg2);
        assertNotSame(msg1, msg2);
        assertTrue(msg1.equals(jmsg) || msg1.equals(cmsg));
        assertTrue(msg2.equals(jmsg) || msg2.equals(cmsg));

        bundleContext.ungetService(refs[0]);
        bundleContext.ungetService(refs[1]);

        // Uninstall it
        simpleServiceDuplicateBundle.uninstall();
        simpleServiceDuplicateBundle = bundleContext.installBundle(getLocator().locateArtifact(
            "org.springframework.osgi", "org.springframework.osgi.iandt.simple.service.identical", getSpringOsgiVersion()).getURL().toExternalForm());

        assertNotNull("Cannot find the simple service duplicate bundle", simpleServiceDuplicateBundle);
        startDependency(simpleServiceDuplicateBundle);

        // do it all again
        refs = bundleContext.getServiceReferences(DEPENDENT_CLASS_NAME, null);

        assertEquals(2, refs.length);

        service1 = (MyService)bundleContext.getService(refs[0]);
        service2 = (MyService)bundleContext.getService(refs[1]);

        assertNotNull(service1);
        assertNotNull(service2);

        msg1 = service1.stringValue();
        msg2 = service2.stringValue();

        System.out.println(msg1);
        System.out.println(msg2);
        assertNotSame(msg1, msg2);
        assertTrue(msg1.equals(jmsg) || msg1.equals(cmsg));
        assertTrue(msg2.equals(jmsg) || msg2.equals(cmsg));
    }

    private void startDependency(Bundle bundle) throws BundleException, InterruptedException {
        bundle.start();
        waitOnContextCreation(bundle.getSymbolicName());
        System.out.println("started bundle [" + OsgiBundleUtils.getNullSafeSymbolicName(bundle) + "]");
    }

    /*
      * (non-Javadoc)
      * @see org.springframework.osgi.test.AbstractSynchronizedOsgiTests#shouldWaitForSpringBundlesContextCreation()
      */
    protected boolean shouldWaitForSpringBundlesContextCreation() {
        return true;
    }



    /* (non-Javadoc)
      * @see org.springframework.osgi.test.AbstractSynchronizedOsgiTests#getDefaultWaitTime()
      */
    protected long getDefaultWaitTime() {
        return 60L;
    }

}
