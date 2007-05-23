package org.springframework.osgi.iandt.annotationProxy;

import org.osgi.framework.Bundle;
import org.springframework.core.JdkVersion;
import org.springframework.osgi.ServiceUnavailableException;
import org.springframework.osgi.iandt.annotation.proxy.ServiceReferer;
import org.springframework.osgi.iandt.simpleservice.MyService;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;

/**
 * @author Andy Piper
 */
public class AnnotationProxyTest extends AbstractConfigurableBundleCreatorTests {

    protected String getManifestLocation() {
        // return
        // "classpath:org/springframework/osgi/test/annotationProxy/ReferenceProxyTest.MF";
        return null;
    }

    protected String[] getBundles() {
        if (JdkVersion.isAtLeastJava15()) {
            return new String[]{
                    localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT"),
                    localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.iandt.simple.service",
                            getSpringOsgiVersion()),
                    localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.iandt.annotation.proxy",
                            getSpringOsgiVersion())};
        } else {
            return new String[]{
                    localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT"),
                    localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.iandt.simple.service",
                            getSpringOsgiVersion())};

        }
    }

    public void testReferenceProxyLifecycle() throws Exception {

        // Skip this test on JDK 1.4
        if (!JdkVersion.isAtLeastJava15()) {
            return;
        }
        MyService reference = ServiceReferer.serviceReference;

        assertNotNull("reference not initialized", reference);
        assertNotNull("no value specified in the reference", reference.stringValue());

        Bundle simpleServiceBundle = findBundleBySymbolicName("org.springframework.osgi.iandt.simpleservice");

        assertNotNull("Cannot find the simple service bundle", simpleServiceBundle);
        System.out.println("stopping bundle");
        simpleServiceBundle.stop();

        while (simpleServiceBundle.getState() == Bundle.STOPPING) {
            System.out.println("waiting for bundle to stop");
            Thread.sleep(10);
        }
        System.out.println("bundle stopped");

        // Service should be unavailable
        try {
            reference.stringValue();
            fail("ServiceUnavailableException should have been thrown!");
        }
        catch (ServiceUnavailableException e) {
            // Expected
        }

        System.out.println("starting bundle");
        simpleServiceBundle.start();

        waitOnContextCreation("org.springframework.osgi.iandt.simpleservice");

        System.out.println("bundle started");
        // Service should be running
        assertNotNull(reference.stringValue());
    }

    /* (non-Javadoc)
      * @see org.springframework.osgi.test.AbstractSynchronizedOsgiTests#getDefaultWaitTime()
      */
    protected long getDefaultWaitTime() {
        return 15L;
    }


}
