package org.example.simple;

import org.springframework.osgi.test.ConfigurableBundleCreatorTests;

/**
 * @author Hal Hildebrand
 *         Date: Oct 14, 2006
 *         Time: 10:16:31 AM
 */
public class FelixTest extends ConfigurableBundleCreatorTests {
    public FelixTest() {
        System.setProperty(OSGI_FRAMEWORK_SELECTOR, FELIX_PLATFORM);
    }


    protected String getManifestLocation() {
        return "classpath:org/example/simple/MANIFEST.MF";
    }


    protected String[] getBundleLocations() {
        return new String[]{
                localMavenArtifact("aopalliance.osgi", "1.0-SNAPSHOT"),
                localMavenArtifact("commons-collections.osgi", "3.2-SNAPSHOT"),
                localMavenArtifact("spring-aop", "2.1-SNAPSHOT"),
                localMavenArtifact("spring-context", "2.1-SNAPSHOT"),
                localMavenArtifact("spring-beans", "2.1-SNAPSHOT"),
                localMavenArtifact("spring-osgi-core", "1.0-SNAPSHOT"),
                localMavenArtifact("spring-jmx", "2.1-SNAPSHOT")
        };
    }


    public void testMe() {
    }
}
