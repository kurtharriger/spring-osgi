package org.springframework.osgi.test.serviceListener;

import org.osgi.framework.Bundle;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;
import org.springframework.osgi.test.service.listener.MyListener;

/**
 * @author Hal Hildebrand
 *         Date: Nov 14, 2006
 *         Time: 8:18:15 AM
 */
public class ServiceListenerTest extends ConfigurableBundleCreatorTests {

    protected String getManifestLocation() {
        return "classpath:org/springframework/osgi/test/serviceListener/ServiceListenerTest.MF";
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
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.service.listener",
                                   "1.0-SNAPSHOT")
        };
    }

    public void testServiceListener() throws Exception {
        assertEquals("Expected initial binding of service", 1, MyListener.BOUND_COUNT);
        assertEquals("Unexpected initial unbinding of service", 0, MyListener.UNBOUND_COUNT);

        Bundle simpleServiceBundle = findBundleBySymbolicName("org.springframework.osgi.samples.simpleservice");

        assertNotNull("Cannot find the simple service bundle", simpleServiceBundle);

        simpleServiceBundle.stop();
        while (simpleServiceBundle.getState() == Bundle.STOPPING) {
            Thread.sleep(10);
        }

        assertEquals("Expected one binding of service", 1, MyListener.BOUND_COUNT);
        assertTrue("Expected only one unbinding of service", MyListener.UNBOUND_COUNT < 2);
        assertEquals("Expected unbinding of service not seen", 1, MyListener.UNBOUND_COUNT);

        simpleServiceBundle.start();
        while (simpleServiceBundle.getState() != Bundle.ACTIVE) {
            Thread.sleep(10);
        }

        assertTrue("Expected only two bindings of service", MyListener.BOUND_COUNT < 3);
        assertEquals("Expected binding of service not seen", 2, MyListener.BOUND_COUNT);
        assertEquals("Unexpected unbinding of service", 1, MyListener.UNBOUND_COUNT);  
    }
}
