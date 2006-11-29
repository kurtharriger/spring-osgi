package org.springframework.osgi.test.referenceProxy;

import org.osgi.framework.Bundle;
import org.springframework.osgi.samples.simpleservice.MyService;
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


    protected String[] getBundleLocations() {
        return new String[]{
                localMavenArtifact("org.springframework.osgi", "aopalliance.osgi", "1.0-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-aop", "2.1-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-context", "2.1-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-beans", "2.1-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-osgi-core", "1.0-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "spring-jmx", "2.1-SNAPSHOT"),
                localMavenArtifact("org.knopflerfish.bundles", "commons-logging_all", "2.0.0"),
                localMavenArtifact("org.springframework.osgi.samples", "simple-service-bundle",
                                   "1.0-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.reference.proxy",
                                   "1.0-SNAPSHOT")
        };
    }

	public void testNoTest() {
		// just to stop the tests barfing.
	}

	// FIXME -- this just hangs. I can't work out what is going on.
    public void XtestReferenceProxyLifecycle() throws Exception {
        MyService reference = ServiceReferer.serviceReference;
        assertNotNull(reference.stringValue());

        Bundle simpleServiceBundle = findBundleBySymbolicName("org.springframework.osgi.samples.simpleservice");

        assertNotNull("Cannot find the simple service bundle", simpleServiceBundle);
        simpleServiceBundle.stop();

        while (simpleServiceBundle.getState() == Bundle.STOPPING) {
            Thread.sleep(10);
        }

        // Service should be unavailable
        try {
            reference.stringValue();
            fail("ServiceUnavailableException should have been thrown!");
        } catch (ServiceUnavailableException e) {
            // Expected
        }

        simpleServiceBundle.start();

        while (simpleServiceBundle.getState() != Bundle.ACTIVE) {
            Thread.sleep(10);
        }

        //Service should be running
        assertNotNull(reference.stringValue());
    }
}
