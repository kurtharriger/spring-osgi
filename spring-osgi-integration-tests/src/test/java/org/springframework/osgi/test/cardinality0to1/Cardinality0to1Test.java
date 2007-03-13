package org.springframework.osgi.test.cardinality0to1;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.springframework.osgi.service.ServiceUnavailableException;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;
import org.springframework.osgi.test.cardinality0to1.test.MyListener;

/**
 * @author Hal Hildebrand
 *         Date: Dec 6, 2006
 *         Time: 6:04:42 PM
 */
public class Cardinality0to1Test extends ConfigurableBundleCreatorTests {

    protected String getManifestLocation() {
        //return "classpath:org/springframework/osgi/test/cardinality0to1/Cardinality0to1Test.MF";
    	return null;
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
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.simple.service",
                                   "1.0-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.cardinality0to1",
                                   "1.0-SNAPSHOT")
        };
    }


    public void test0to1Cardinality() throws Exception {
        waitOnContextCreation("org.springframework.osgi.test.simpleservice");
        waitOnContextCreation("org.springframework.osgi.test.cardinality.0to1");
        BundleContext bundleContext = getBundleContext();

        Bundle simpleService2Bundle = bundleContext.installBundle(
                getLocator().locateArtifact("org.springframework.osgi", "org.springframework.osgi.test.simple.service2",
                                   "1.0-SNAPSHOT").getURL().toExternalForm());

        assertNotNull("Cannot find the simple service 2 bundle", simpleService2Bundle);

        assertNotSame("simple service 2 bundle is in the activated state!",
                      new Integer(Bundle.ACTIVE),
                      new Integer(simpleService2Bundle.getState()));


        assertEquals("Unxpected initial binding of service", 0, MyListener.BOUND_COUNT);
        assertEquals("Unexpected initial unbinding of service", 0, MyListener.UNBOUND_COUNT);
        assertNotNull("Service reference should be not null", MyListener.service);

        try {
            MyListener.service.stringValue();
            fail("Service should be unavailable");
        } catch (ServiceUnavailableException e) {
            // expected
        }

        startDependency(simpleService2Bundle);


        assertEquals("Expected initial binding of service", 1, MyListener.BOUND_COUNT);
        assertEquals("Unexpected initial unbinding of service", 0, MyListener.UNBOUND_COUNT);
        assertNotNull("Service reference should be not null", MyListener.service);

        assertNotNull(MyListener.service.stringValue());
        
    }


    private void startDependency(Bundle simpleService2Bundle) throws BundleException, InterruptedException {
        System.out.println("Starting dependency");
        simpleService2Bundle.start();

        waitOnContextCreation("org.springframework.osgi.test.simpleservice2");

        System.out.println("Dependency started");
    }
}
