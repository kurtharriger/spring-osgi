package org.springframework.osgi.test.referenceProxy;

import org.osgi.framework.Bundle;
import org.springframework.osgi.test.simpleservice.MyService;
import org.springframework.osgi.service.ServiceUnavailableException;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;
import org.springframework.osgi.test.reference.proxy.ServiceReferer;

/**
 * @author Hal Hildebrand
 *         Date: Nov 25, 2006
 *         Time: 12:42:30 PM
 */
public class ReferenceProxyTest extends ConfigurableBundleCreatorTests {

    protected String getManifestLocation() {
        return "classpath:org/springframework/osgi/test/referenceProxy/ReferenceProxyTest.MF";
    }


    protected String[] getBundles() {
        return new String[]{
                localMavenArtifact("org.springframework.osgi", "aopalliance.osgi", "1.0-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-aop", "2.1-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-context", "2.1-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-beans", "2.1-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-osgi-core", "1.0-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-osgi-extender", "1.0-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-jmx", "2.1-SNAPSHOT"), 
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.simple.service",
                                   "1.0-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.reference.proxy",
                                   "1.0-SNAPSHOT")
        };
    }

    public void testReferenceProxyLifecycle() throws Exception {
    	waitOnContextCreation("org.springframework.osgi.test.simpleservice");
    	waitOnContextCreation("org.springframework.osgi.test.reference.proxy");
        MyService reference = ServiceReferer.serviceReference;
        assertNotNull(reference.stringValue());

        Bundle simpleServiceBundle = findBundleBySymbolicName("org.springframework.osgi.test.simpleservice");

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
        } catch (ServiceUnavailableException e) {
            // Expected
        }

        System.out.println("starting bundle");
        simpleServiceBundle.start();

    	waitOnContextCreation("org.springframework.osgi.test.simpleservice");

        System.out.println("bundle started");
        //Service should be running
        assertNotNull(reference.stringValue());
    }
}
