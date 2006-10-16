package org.springframework.osgi.test.lifecycle;

import org.osgi.framework.Bundle;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;

/**
 * @author Hal Hildebrand
 *         Date: Oct 15, 2006
 *         Time: 5:51:36 PM
 */
public class LifecycleTest extends ConfigurableBundleCreatorTests {

    static {
        System.setProperty(OSGI_FRAMEWORK_SELECTOR, EQUINOX_PLATFORM);
    }


    protected String getManifestLocation() {
        return "classpath:org/springframework/osgi/test/lifecycle/MANIFEST.MF";
    }


    protected String[] getBundleLocations() {
        return new String[]{
                localMavenArtifact("aopalliance.osgi", "1.0-SNAPSHOT"),
                localMavenArtifact("commons-collections.osgi", "3.2-SNAPSHOT"),
                localMavenArtifact("spring-aop", "2.1-SNAPSHOT"),
                localMavenArtifact("spring-context", "2.1-SNAPSHOT"),
                localMavenArtifact("spring-beans", "2.1-SNAPSHOT"),
                localMavenArtifact("spring-osgi-core", "1.0-SNAPSHOT"),
                localMavenArtifact("spring-jmx", "2.1-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi.test.lifecycle", "1.0-SNAPSHOT")

        };
    }


    public void testLifecycle() throws Exception {
        assertNotSame("Guinea pig has already been shutdown", "true",
                      System.getProperty("org.springframework.osgi.test.lifecycle.GuineaPig.close"));

        assertEquals("Guinea pig didn't startup", "true",
                     System.getProperty("org.springframework.osgi.test.lifecycle.GuineaPig.startUp"));
        Bundle[] bundles = getBundleContext().getBundles();
        Bundle testBundle = null;
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].getSymbolicName().equals("org.springframework.osgi.test.lifecycle")) {
                testBundle = bundles[i];
                break;
            }
        }
        assertNotNull("Could not find the test bundle", testBundle);
        testBundle.stop();
        assertEquals("Guinea pig didn't shutdown", "true",
                     System.getProperty("org.springframework.osgi.test.lifecycle.GuineaPig.close"));
    }
}
