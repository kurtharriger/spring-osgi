package org.springframework.osgi.iandt.dependency;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Crucial test for the asych, service-dependency waiting. Installs several
 * bundles which depend on each other services making sure that none of them
 * starts unless the dependent bundle (and its services) are also started.
 * 
 * @author Hal Hildebrand Date: Dec 1, 2006 Time: 3:56:43 PM
 * @author Costin Leau
 */
public class DependencyTest extends AbstractConfigurableBundleCreatorTests {
	private static final String DEPENDENT_CLASS_NAME = "org.springframework.osgi.iandt.dependencies.Dependent";

	// private static final String SERVICE_2_FILTER = "(service=2)";
	// private static final String SERVICE_3_FILTER = "(service=3)";

	protected String getManifestLocation() {
		return "classpath:org/springframework/osgi/iandt/dependency/DependencyTest.MF";
	}

	protected String[] getBundles() {
		return new String[] { localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT") };
	}

	// dependency bundle - depends on service2, service3 and, through a nested reference, to service1
	// simple.service2 - publishes service2
	// simple.service3 - publishes service3
	// simple 		   - publishes service
	public void testDependencies() throws Exception {
		// waitOnContextCreation("org.springframework.osgi.iandt.simpleservice");

		BundleContext bundleContext = getBundleContext();

		Bundle dependencyTestBundle = bundleContext.installBundle(getLocator().locateArtifact(
			"org.springframework.osgi", "org.springframework.osgi.iandt.dependencies", getSpringOsgiVersion()).getURL().toExternalForm());

		Bundle simpleService2Bundle = bundleContext.installBundle(getLocator().locateArtifact(
			"org.springframework.osgi", "org.springframework.osgi.iandt.simple.service2", getSpringOsgiVersion()).getURL().toExternalForm());
		Bundle simpleService3Bundle = bundleContext.installBundle(getLocator().locateArtifact(
			"org.springframework.osgi", "org.springframework.osgi.iandt.simple.service3", getSpringOsgiVersion()).getURL().toExternalForm());
		Bundle simpleServiceBundle = bundleContext.installBundle(getLocator().locateArtifact(
			"org.springframework.osgi", "org.springframework.osgi.iandt.simple.service", getSpringOsgiVersion()).getURL().toExternalForm());

		assertNotNull("Cannot find the simple service bundle", simpleServiceBundle);
		assertNotNull("Cannot find the simple service 2 bundle", simpleService2Bundle);
		assertNotNull("Cannot find the simple service 3 bundle", simpleService3Bundle);
		assertNotNull("dependencyTest can't be resolved", dependencyTestBundle);

		assertNotSame("simple service bundle is in the activated state!", new Integer(Bundle.ACTIVE), new Integer(
				simpleServiceBundle.getState()));

		assertNotSame("simple service 2 bundle is in the activated state!", new Integer(Bundle.ACTIVE), new Integer(
				simpleService2Bundle.getState()));

		assertNotSame("simple service 3 bundle is in the activated state!", new Integer(Bundle.ACTIVE), new Integer(
				simpleService3Bundle.getState()));

		startDependencyAsynch(dependencyTestBundle);
		Thread.sleep(2000); // Yield to give bundle time to get into waiting
		// state.
		ServiceReference dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

		assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

		startDependency(simpleService3Bundle);

		dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

		assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

		startDependency(simpleService2Bundle);

		assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

		dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

		startDependency(simpleServiceBundle);

		assertNull("Service with unsatisfied dependencies has been started!", dependentRef);

		waitOnContextCreation("org.springframework.osgi.iandt.dependencies");

		dependentRef = bundleContext.getServiceReference(DEPENDENT_CLASS_NAME);

		assertNotNull("Service has not been started!", dependentRef);

		Object dependent = bundleContext.getService(dependentRef);

		assertNotNull("Service is not available!", dependent);

	}

	private void startDependency(Bundle bundle) throws BundleException, InterruptedException {
		bundle.start();
		waitOnContextCreation(bundle.getSymbolicName());
		System.out.println("started bundle [" + OsgiStringUtils.nullSafeSymbolicName(bundle) + "]");
	}

	private void startDependencyAsynch(final Bundle bundle) {
		System.out.println("starting dependency test bundle");
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					bundle.start();
					System.out.println("started dependency test bundle");
				}
				catch (BundleException ex) {
					System.err.println("can't start bundle " + ex);
				}
			}
		};
		Thread thread = new Thread(runnable);
		thread.setDaemon(false);
		thread.setName("dependency test bundle");
		thread.start();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.test.AbstractSynchronizedOsgiTests#shouldWaitForSpringBundlesContextCreation()
	 */
	protected boolean shouldWaitForSpringBundlesContextCreation() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.test.AbstractSynchronizedOsgiTests#getDefaultWaitTime()
	 */
	protected long getDefaultWaitTime() {
		return 60L;
	}

}
