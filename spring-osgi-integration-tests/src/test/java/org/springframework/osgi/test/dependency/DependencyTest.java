package org.springframework.osgi.test.dependency;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.BundleException;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;

/**
 * @author Hal Hildebrand
 *         Date: Dec 1, 2006
 *         Time: 3:56:43 PM
 */
public class DependencyTest extends ConfigurableBundleCreatorTests {
    private static final String DEPENDENT_CLASS_NAME = "org.springframework.osgi.test.dependencies.Dependent";
    private static final String SERVICE_2_FILTER = "(service=2)";
    private static final String SERVICE_3_FILTER = "(service=3)";


    protected String getManifestLocation() {
        return "classpath:org/springframework/osgi/test/dependency/DependencyTest.MF";
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
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.simple.service",
                                   "1.0-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.dependencies",
                                   "1.0-SNAPSHOT")
        };
    }


    public void testDependencies() throws Exception {
        BundleContext bundleContext = getBundleContext();

        Bundle simpleService2Bundle = bundleContext.installBundle(
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.simple.service2",
                                   "1.0-SNAPSHOT"));
        Bundle simpleService3Bundle = bundleContext.installBundle(
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test.simple.service3",
                                   "1.0-SNAPSHOT"));

        assertNotNull("Cannot find the simple service 2 bundle", simpleService2Bundle);
        assertNotNull("Cannot find the simple service 3 bundle", simpleService3Bundle);

        assertNotSame("simple service 2 bundle is in the activated state!",
                      new Integer(Bundle.ACTIVE),
                      new Integer(simpleService2Bundle.getState()));

        assertNotSame("simple service 3 bundle is in the activated state!",
                      new Integer(Bundle.ACTIVE),
                      new Integer(simpleService3Bundle.getState()));

        ServiceReference dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

        assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

        startSecondDependency(simpleService3Bundle);

        dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

        assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

        startFirstDependency(simpleService2Bundle);
        
        // Context switch to allow service to start up...
        Thread.sleep(1000);

        dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

        assertNotNull("Service has not been started!", dependentRef);

        Object dependent = bundleContext.getService(dependentRef);

        assertNotNull("Service is not available!", dependent);

    }


    private void startFirstDependency(Bundle simpleService2Bundle) throws BundleException, InterruptedException {
        System.out.println("Starting first dependency");
        simpleService2Bundle.start();

        while (simpleService2Bundle.getState() != Bundle.ACTIVE) {
            System.out.println("Waiting for first dependency to start");
            Thread.sleep(10);
        }
        System.out.println("First dependency started");
    }


    private void startSecondDependency(Bundle simpleService3Bundle) throws BundleException, InterruptedException {
        System.out.println("Starting second dependency");
        simpleService3Bundle.start();

        while (simpleService3Bundle.getState() != Bundle.ACTIVE) {
            System.out.println("Waiting for second dependency to start");
            Thread.sleep(10);
        }
        System.out.println("Second dependency started");
    }
}
