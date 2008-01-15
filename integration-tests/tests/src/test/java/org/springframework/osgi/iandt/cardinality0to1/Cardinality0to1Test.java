
package org.springframework.osgi.iandt.cardinality0to1;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.springframework.osgi.iandt.BaseIntegrationTest;
import org.springframework.osgi.iandt.cardinality0to1.test.MyListener;
import org.springframework.osgi.iandt.cardinality0to1.test.ReferenceContainer;
import org.springframework.osgi.service.ServiceUnavailableException;

/**
 * @author Hal Hildebrand Date: Dec 6, 2006 Time: 6:04:42 PM
 */
public class Cardinality0to1Test extends BaseIntegrationTest {

	protected String[] getTestBundlesNames() {
		return new String[] {
			"org.springframework.osgi, org.springframework.osgi.iandt.simple.service," + getSpringDMVersion(),
			"org.springframework.osgi, org.springframework.osgi.iandt.cardinality0to1," + getSpringDMVersion() };
	}

	public void test0to1Cardinality() throws Exception {
		Bundle simpleService2Bundle = bundleContext.installBundle(getLocator().locateArtifact(
			"org.springframework.osgi", "org.springframework.osgi.iandt.simple.service2", getSpringDMVersion()).getURL().toExternalForm());

		assertNotNull("Cannot find the simple service 2 bundle", simpleService2Bundle);

		assertNotSame("simple service 2 bundle is in the activated state!", new Integer(Bundle.ACTIVE), new Integer(
			simpleService2Bundle.getState()));

		assertEquals("Unxpected initial binding of service", 0, MyListener.BOUND_COUNT);
		assertEquals("Unexpected initial unbinding of service", 0, MyListener.UNBOUND_COUNT);
		assertNotNull("Service reference should be not null", ReferenceContainer.service);

		try {
			ReferenceContainer.service.stringValue();
			fail("Service should be unavailable");
		}
		catch (ServiceUnavailableException e) {
			// expected
		}

		startDependency(simpleService2Bundle);

		assertEquals("Expected initial binding of service", 1, MyListener.BOUND_COUNT);
		assertEquals("Unexpected initial unbinding of service", 0, MyListener.UNBOUND_COUNT);
		assertNotNull("Service reference should be not null", ReferenceContainer.service);

		assertNotNull(ReferenceContainer.service.stringValue());

	}

	private void startDependency(Bundle simpleService2Bundle) throws BundleException, InterruptedException {
		System.out.println("Starting dependency");
		simpleService2Bundle.start();

		waitOnContextCreation("org.springframework.osgi.iandt.simpleservice2");

		System.out.println("Dependency started");
	}
}
