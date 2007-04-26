/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.osgi.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import junit.framework.Protectable;
import junit.framework.TestCase;
import junit.framework.TestResult;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.io.OsgiBundleResourceLoader;
import org.springframework.osgi.test.platform.OsgiPlatform;
import org.springframework.osgi.test.util.ConfigurableByteArrayOutputStream;
import org.springframework.osgi.test.util.IOUtils;
import org.springframework.osgi.test.util.TestUtils;
import org.springframework.util.Assert;

/**
 * 
 * Base test for OSGi environments. Takes care of starting the OSGi platform,
 * installing the given bundles and delegating the test execution to a copy
 * which runs inside OSGi.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractOsgiTests extends AbstractOptionalDependencyInjectionTests implements OsgiJUnitTest {

	// JVM shutdown hook
	private static Thread shutdownHook;

	// the OSGi fixture
	private static OsgiPlatform osgiPlatform;

	// OsgiPlatform bundle context
	private static BundleContext platformContext;

	// JUnit Service
	private static Object service;

	// JUnitService trigger
	private static Method serviceTrigger;

	// streams for communicating with the OSGi realm.
	private ObjectInputStream inputStream;

	private ObjectOutputStream outputStream;

	// the test results used by the triggering test runner
	private TestResult originalResult;

	// The OSGi BundleContext (when executing the test as a bundle inside OSGi)
	private BundleContext bundleContext;

	// OsgiResourceLoader
	private ResourceLoader resourceLoader;

	/**
	 * Hook for JUnit infrastructures which can't reuse this class hierarchy.
	 * This instance represents the test which will be executed by
	 * AbstractOsgiTests & co.
	 */
	private TestCase osgiJUnitTest = this;

	private static final String ACTIVATOR_REFERENCE = "org.springframework.osgi.test.JUnitTestActivator";

	public AbstractOsgiTests() {
		super();
	}

	public AbstractOsgiTests(String name) {
		super(name);
	}

	protected ConfigurableApplicationContext createApplicationContext(String[] locations) {
		ConfigurableApplicationContext context = new OsgiBundleXmlApplicationContext(getBundleContext(), locations);
		context.refresh();
		return context;
	}

	/**
	 * Bundles that should be installed before the test execution.
	 * 
	 * @return the array of bundles to install
	 */
	protected String[] getBundles() {
		return new String[0];
	}

	/**
	 * Legacy method - will be removed in future SVN revisions.
	 * @return
	 */
	protected String[] getBundleLocations() {
		return getBundles();
	}

	/**
	 * Mandator bundles (part of the test setup).
	 * 
	 * @return the array of mandatory bundle names.
	 */
	protected abstract String[] getMandatoryBundles();

	/**
	 * Return the resource loader used by this test.
	 * 
	 * @return an OsgiBundleResourceLoader if the bundleContext was set or null
	 * otherwise.
	 */
	protected ResourceLoader getResourceLoader() {
		return resourceLoader;
	}

	/**
	 * Return the bundleContext for the bundle in which this test is running.
	 * 
	 * @return
	 */
	protected BundleContext getBundleContext() {
		return bundleContext;
	}

	/**
	 * Create (and configure) the OSGi platform.
	 * 
	 * @return OSGi platform.
	 */
	protected abstract OsgiPlatform createPlatform();

	/**
	 * Callback for processing the platform bundle context before any bundles
	 * have been installed. The method is invoked <b>after</b> starting the
	 * OSGi environment but <b>before</b> any bundles are installed in the OSGi
	 * framework.
	 * 
	 * Normally, this method is called only one during the lifecycle of a test
	 * suite.
	 * 
	 * @see #postProcessBundleContext(BundleContext)
	 * @param platformBundleContext
	 * @throws Exception
	 */
	protected void preProcessBundleContext(BundleContext platformBundleContext) throws Exception {
	}

	/**
	 * Callback for processing the platform bundle context after the critical
	 * test infrastructure bundles have been installed and started. The method
	 * is invoked <b>after</b> preparing the OSGi environment for the test
	 * execution but <b>before</b> any test is executed.
	 * 
	 * The given BundleContext belongs to the underlying OSGi framework.
	 * 
	 * Normally, this method is called only one during the lifecycle of a test
	 * suite.
	 * 
	 * <p/> Note that at this point, {@link #getBundleContext()} returns null
	 * since the OSGi test execution hasn't started yet - the only BundleContext
	 * available is that of the OSGi framework.
	 * 
	 * @see #preProcessBundleContext(BundleContext)
	 * @param platformBundleContext
	 */
	protected void postProcessBundleContext(BundleContext platformBundleContext) throws Exception {
	}

	//
	// JUnit overriden methods.
	//

	/**
	 * Replacement run method. Get a hold of the TestRunner used for running
	 * this test so it can populate it with the results retrieved from OSGi.
	 * 
	 */
	public final void run(TestResult result) {

		// get a hold of the test result
		originalResult = result;

		// TODO: can this be actually improved (can we still reuse the
		// testResult)
		result.startTest(osgiJUnitTest);
		result.runProtected(osgiJUnitTest, new Protectable() {
			public void protect() throws Throwable {
				AbstractOsgiTests.this.runBare();
			}
		});
		result.endTest(osgiJUnitTest);

		// super.run(result);
	}

	public void runBare() throws Throwable {
		prepareTestExecution();
		try {
			// invoke OSGi test run
			invokeOSGiTestExecution();
			readTestResult();
		}
		finally {
			completeTestExecution();
		}
	}

	//
	// OsgiJUnitTest execution hooks.
	//

	/**
	 * the setUp version for the OSGi environment.
	 * 
	 * @throws Exception
	 */
	public final void osgiSetUp() throws Exception {
		// call the normal onSetUp
		setUp();
	}

	public final void osgiTearDown() throws Exception {
		// call the normal tearDown
		tearDown();
	}

	/**
	 * Actual test execution (delegates to the superclass implementation).
	 * 
	 * @throws Throwable
	 */
	public final void osgiRunTest() throws Throwable {
		super.runTest();
	}

	//
	// Delegation methods for OSGi execution.
	//

	/**
	 * Prepare test execution - the OSGi platform will be started (if needed)
	 * and cached for the test suite execution.
	 * 
	 */
	private void prepareTestExecution() throws Exception {

		if (getName() == null)
			throw new IllegalArgumentException("no test specified");

		// write the testname into the System properties
		System.getProperties().put(OsgiJUnitTest.OSGI_TEST, osgiJUnitTest.getClass().getName());

		// create streams first to avoid deadlocks in setting up the stream on
		// the OSGi side
		setupStreams();

		// start OSGi platform (the caching is done inside the method).
		try {
			startup();
		}
		catch (Exception e) {
			logger.debug("Caught exception starting up", e);
			throw e;
		}

		logger.debug("writing test name [" + getName() + "] to OSGi");
		// write test name to OSGi
		outputStream.writeUTF(getName());
		outputStream.flush();

	}

	/**
	 * Delegate the test execution to the OSGi copy.
	 * 
	 * @throws Exception
	 */
	private void invokeOSGiTestExecution() throws Exception {
		try {
			serviceTrigger.invoke(service, null);
		}
		catch (InvocationTargetException ex) {
			Throwable th = ex.getCause();
			if (th instanceof Exception)
				throw ((Exception) th);
			else
				throw ((Error) th);
		}
	}

	/**
	 * Finish the test execution - read back the result from the OSGi copy and
	 * closes up the streams.
	 * 
	 * @throws Exception
	 */
	private void completeTestExecution() {
		IOUtils.closeStream(inputStream);
		IOUtils.closeStream(outputStream);
	}

	//
	// OSGi testing infrastructure setup.
	//

	/**
	 * Start the OSGi platform and install/start the bundles (happens once for
	 * the all test runs)
	 * 
	 * @throws Exception
	 */
	private void startup() throws Exception {
		if (osgiPlatform == null) {

			// make sure the platform is closed properly
			registerShutdownHook();

			osgiPlatform = createPlatform();
			// start platform
			logger.debug("about to start " + osgiPlatform);
			osgiPlatform.start();
			// platform context
			platformContext = osgiPlatform.getBundleContext();

			// log platform name and version
			logPlatformInfo(platformContext);

			// hook before the OSGi platform is setup but right after is has
			// been started
			preProcessBundleContext(platformContext);

			// install bundles (from the local system/classpath)
			Resource[] bundleResources = locateBundles();

			Bundle[] bundles = new Bundle[bundleResources.length];
			for (int i = 0; i < bundles.length; i++) {
				bundles[i] = installBundle(bundleResources[i]);
			}

			// start bundles
			for (int i = 0; i < bundles.length; i++) {
				logger.debug("starting bundle " + bundles[i].getBundleId() + "[" + bundles[i].getSymbolicName() + "]");
				try {
					bundles[i].start();
				}
				catch (Throwable ex) {
					logger.warn("can't start bundle " + bundles[i].getBundleId() + "[" + bundles[i].getSymbolicName()
							+ "]", ex);
				}

			}
			
			// hook after the OSGi platform has been setup
			postProcessBundleContext(platformContext);

			initializeServiceRunnerInvocationMethods();
		}
	}

	/**
	 * Log the underlying OSGi information (which can be tricky).
	 * 
	 */
	private void logPlatformInfo(BundleContext context) {
		StringBuffer platformInfo = new StringBuffer();

		// add platform information
		platformInfo.append(osgiPlatform);
		// get current bundle (it has to be the system bundle since we just
		// bootstrapped the platform)
		Bundle sysBundle = context.getBundle();

		platformInfo.append(" [");
		// Version
		platformInfo.append(sysBundle.getHeaders().get(Constants.BUNDLE_VERSION));
		platformInfo.append("]");
		logger.info(platformInfo + " started");
	}

	/**
	 * Load the bundles given as strings. Will delegate to
	 * {@link #locateBundle(String)}.
	 * 
	 * @param bundles
	 * @return
	 */
	protected Resource[] locateBundles(String[] bundles) {
		Resource[] res = new Resource[bundles.length];
		// ResourceLoader loader = new DefaultResourceLoader();

		for (int i = 0; i < bundles.length; i++) {
			// res[i] = loader.getResource(bundles[i]);
			res[i] = locateBundle(bundles[i]);
		}
		return res;
	}

	protected Resource[] locateBundles() {
		// merge bundles
		String[] mandatoryBundles = getMandatoryBundles();
		String[] optionalBundles = getBundles();

		String[] allBundles = new String[mandatoryBundles.length + optionalBundles.length];
		System.arraycopy(mandatoryBundles, 0, allBundles, 0, mandatoryBundles.length);
		System.arraycopy(optionalBundles, 0, allBundles, mandatoryBundles.length, optionalBundles.length);

		return locateBundles(allBundles);
	}

	/**
	 * Locate (through a Resource) an OSGi bundle given by a String. Subclasses
	 * should provide an implementation to this method.
	 * 
	 * Note that the String identifying the bundle can have arbitrary format
	 * (such as Comma Separated Values) dependening on the strategy used.
	 * 
	 * @param bundleId
	 * @return
	 */
	protected abstract Resource locateBundle(String bundleId);

	/**
	 * Install an OSGi bundle from the given location.
	 * 
	 * @param location
	 * @return
	 * @throws Exception
	 */
	private Bundle installBundle(Resource location) throws Exception {
		Assert.notNull(platformContext);
		Assert.notNull(location);
		logger.debug("installing bundle from location " + location.getDescription());
		return platformContext.installBundle(location.getDescription(), location.getInputStream());
	}

	/**
	 * Determine through reflection the methods used for invoking the
	 * TestRunnerService.
	 * 
	 * @throws Exception
	 */
	private void initializeServiceRunnerInvocationMethods() throws Exception {
		// get JUnit test service reference
		// this is a loose reference - update it if the JUnitTestActivator
		// class is
		// changed.
		ServiceReference reference = platformContext.getServiceReference(ACTIVATOR_REFERENCE);
		if (reference == null)
			throw new IllegalStateException("no OSGi service reference found at " + ACTIVATOR_REFERENCE);

		service = platformContext.getService(reference);
		if (service == null) {
			throw new IllegalStateException("no service found for reference: " + reference);
		}

		serviceTrigger = service.getClass().getDeclaredMethod("executeTest", null);
		if (serviceTrigger == null) {
			throw new IllegalStateException("no executeTest() method found on: " + service.getClass());
		}

	}

	private void readTestResult() {
		// finish stream creation (to avoid circular dependencies)
		createInStream();

		logger.debug("reading OSGi results for test [" + getName() + "]");

		TestUtils.receiveTestResult(this.originalResult, osgiJUnitTest, inputStream);

		logger.debug("test[" + getName() + "]'s result read");
	}

	/**
	 * Special shutdown hook.
	 */
	private void registerShutdownHook() {
		if (shutdownHook == null) {
			// No shutdown hook registered yet.
			shutdownHook = new Thread() {
				public void run() {
					shutdownTest();
				}
			};
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
	}

	/**
	 * Cleanup for the test suite.
	 */
	private void shutdownTest() {
		IOUtils.closeStream(inputStream);
		IOUtils.closeStream(outputStream);

		logger.info("shutting down OSGi platform");
		if (osgiPlatform != null) {
			try {
				osgiPlatform.stop();
			}
			catch (Exception ex) {
				// swallow
				logger.warn("shutdown procedure threw exception " + ex);
			}
			osgiPlatform = null;
		}
	}

	/**
	 * Setup the piped streams to get the results out from the OSGi world.
	 * 
	 * @throws IOException
	 */
	private void setupStreams() throws IOException {

		byte[] inArray = new byte[1024 * 2];
		// 16K seems to be enough
		byte[] outArray = new byte[1024 * 16];

		Properties systemProps = System.getProperties();

		// put information for OSGi
		systemProps.put(OsgiJUnitTest.FOR_OSGI, inArray);

		// get information from OSGi
		systemProps.put(OsgiJUnitTest.FROM_OSGI, outArray);

		// setup output stream to prevent blocking
		outputStream = new ObjectOutputStream(new ConfigurableByteArrayOutputStream(inArray));
		// flush header right away
		outputStream.flush();

		logger.debug("opened writer to OSGi");
	}

	/**
	 * Utility method for creating the input communication stream.
	 */
	private void createInStream() {
		try {
			byte[] inputSource = (byte[]) System.getProperties().get(OsgiJUnitTest.FROM_OSGI);
			inputStream = new ObjectInputStream(new ByteArrayInputStream(inputSource));
			logger.debug("opened reader from OSGi");
		}
		catch (IOException ex) {
			throw new RuntimeException("cannot open streams; it's likely the osgi test execution failed;" + ex);
		}

	}

	/**
	 * Set the bundle context to be used by this test. This method is called
	 * automatically by the test infrastructure after the OSGi platform is being
	 * setup.
	 */
	public final void injectBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		// instantiate ResourceLoader
		this.resourceLoader = new OsgiBundleResourceLoader(bundleContext.getBundle());
	}

	public void setName(String name) {
		super.setName(name);
	}

	/**
	 * Set the underlying OsgiJUnitTest used for the test delegation.
	 * 
	 * @param test
	 */
	public void injectOsgiJUnitTest(OsgiJUnitTest test) {
		Assert.isInstanceOf(TestCase.class, test);
		this.osgiJUnitTest = (TestCase) test;
	}
}
