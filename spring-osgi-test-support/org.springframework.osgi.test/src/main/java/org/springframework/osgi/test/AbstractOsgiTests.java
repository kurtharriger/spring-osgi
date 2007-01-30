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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.osgi.io.OsgiBundleResourceLoader;
import org.springframework.osgi.test.platform.EquinoxPlatform;
import org.springframework.osgi.test.platform.FelixPlatform;
import org.springframework.osgi.test.platform.KnopflerfishPlatform;
import org.springframework.osgi.test.platform.OsgiPlatform;
import org.springframework.util.Assert;

import edu.emory.mathcs.backport.java.util.concurrent.BrokenBarrierException;
import edu.emory.mathcs.backport.java.util.concurrent.CyclicBarrier;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * 
 * Base teste for OSGi environments. It will start the OSGi platform, install
 * the given bundles and then delegate the execution to a test copy which
 * executes inside OSGi.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractOsgiTests extends TestCase implements OsgiJUnitTest {

	// JVM shutdown hook
	private static Thread shutdownHook;

	// the OSGi fixture
	private static OsgiPlatform osgiPlatform;

	private static BundleContext platformContext;

	// JUnit Service
	private static Object service;

	// JUnitService trigger
	private static Method serviceTrigger;

	private ObjectInputStream inputStream;

	private ObjectOutputStream outputStream;

	// the test results used by the triggering test runner
	private TestResult originalResult;

	// The OSGi BundleContext
	private BundleContext bundleContext;

	// OsgiResourceLoader
	private ResourceLoader resourceLoader;

	private static final String ACTIVATOR_REFERENCE = "org.springframework.osgi.test.JUnitTestActivator";

	public static final String EQUINOX_PLATFORM = "equinox";

	public static final String KNOPFLERFISH_PLATFORM = "knopflerfish";

	public static final String FELIX_PLATFORM = "felix";

	public static final String OSGI_FRAMEWORK_SELECTOR = "org.springframework.osgi.test.framework";

	protected final Log log = LogFactory.getLog(getClass());
    private static final String ORG_OSGI_FRAMEWORK_BOOTDELEGATION = "org.osgi.framework.bootdelegation";


    protected String getSpringOSGiTestBundleUrl() {
		return localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.test", "1.0-SNAPSHOT");
	}

	protected String getSpringOSGiIoBundleUrl() {
		return localMavenArtifact("org.springframework.osgi", "spring-osgi-io", "1.0-SNAPSHOT");
	}

	protected String getSpringCoreBundleUrl() {
		return localMavenArtifact("org.springframework.osgi", "spring-core", "2.1-SNAPSHOT");
	}

	protected String getJUnitLibUrl() {
		return localMavenArtifact("org.springframework.osgi", "junit.osgi", "3.8.1-SNAPSHOT");
	}

	protected String getUtilConcurrentLibUrl() {
		return localMavenArtifact("org.springframework.osgi", "backport-util-concurrent", "3.0-SNAPSHOT");
	}

	protected String getJclOverSlf4jUrl() {
		return localMavenArtifact("org.springframework.osgi", "jcl104-over-slf4j.osgi", "1.1.0");
	}

	protected String getSlf4jLog4jUrl() {
		return localMavenArtifact("org.slf4j", "slf4j-log4j-full", "1.1.0");
	}

	protected String getLog4jLibUrl() {
		System.setProperty("log4j.ignoreTCL", "true");
		return localMavenArtifact("org.springframework.osgi", "log4j.osgi", "1.2.13-SNAPSHOT");
	}

	/**
	 * Find a local maven artifact. First tries to find the resource as a
	 * packaged artifact produced by a local maven build, and if that fails will
	 * search the local maven repository.
	 * 
	 * @param groupId - the groupId of the organization supplying the bundle
	 * @param artifactId - the artifact id of the bundle
	 * @param version - the version of the bundle
	 * @return the String representing the URL location of this bundle
	 */
	protected String localMavenArtifact(String groupId, String artifactId, String version) {
		return localMavenArtifact(groupId, artifactId, version, "jar");
	}

	/**
	 * Find a local maven artifact. First tries to find the resource as a
	 * packaged artifact produced by a local maven build, and if that fails will
	 * search the local maven repository.
	 *
	 * @param groupId - the groupId of the organization supplying the bundle
	 * @param artifactId - the artifact id of the bundle
	 * @param version - the version of the bundle
     * @param type - the extension type of the artifact
	 * @return the String representing the URL location of this bundle
	 */
	protected String localMavenArtifact(String groupId, String artifactId, String version, String type) {
		try {
			return localMavenBuildArtifact(artifactId, version, type);
		}
		catch (IllegalStateException illStateEx) {
			return localMavenBundle(groupId, artifactId, version, type);
		}
	}

	/**
	 * Answer the url string of the indicated bundle in the local Maven
	 * repository
	 * 
	 * @param groupId - the groupId of the organization supplying the bundle
	 * @param artifact - the artifact id of the bundle
	 * @param version - the version of the bundle
	 * @return the String representing the URL location of this bundle
	 */
	protected String localMavenBundle(String groupId, String artifact, String version, String type) {
        String defaultHome = new File(new File(System.getProperty("user.home")), ".m2/repository").getAbsolutePath();
        File repositoryHome = new File(System.getProperty("localRepository", defaultHome));
        
        String location = groupId.replace('.', '/');
		location += '/';
		location += artifact;
		location += '/';
		location += version;
		location += '/';
		location += artifact;
		location += '-';
		location += version;
		location += ".";
        location += type;
        return "file:" + new File(repositoryHome, location).getAbsolutePath();
	}

	/**
	 * Find a local maven artifact in the current build tree. This searches for
	 * resources produced by the package phase of a maven build.
	 * 
	 * @param artifactId
	 * @param version
     * @param type
	 * @return a String representing the URL location of this bundle
	 */
	protected String localMavenBuildArtifact(String artifactId, String version, String type) {
		try {
			File found = new MavenPackagedArtifactFinder(artifactId, version, type).findPackagedArtifact(new File("."));
			String path = found.toURL().toExternalForm();
			if (log.isDebugEnabled()) {
				log.debug("found local maven artifact " + path + " for " + artifactId + "|" + version);
			}
			return path;
		}
		catch (IOException ioEx) {
			throw new IllegalStateException("Artifact " + artifactId + "-" + version + "." + type + " could not be found",
					ioEx);
		}
	}

	/**
	 * Bundles that should be installed before the test execution.
	 * 
	 * @return the array of bundles to install
	 */
	protected String[] getBundleLocations() {
		return new String[] {};
	}

	/**
	 * Mandator bundles (part of the test setup).
	 * 
	 * @return the array of mandatory bundle names (sans Log4J, which gets
	 * special handling
	 */
	protected String[] getMandatoryBundles() {
		return new String[] { getJclOverSlf4jUrl(), getSlf4jLog4jUrl(), getLog4jLibUrl(), getJUnitLibUrl(),
				getSpringCoreBundleUrl(), getUtilConcurrentLibUrl(), getSpringOSGiIoBundleUrl(),
				getSpringOSGiTestBundleUrl() };
	}

	public AbstractOsgiTests() {
		super();
	}

	public AbstractOsgiTests(String name) {
		super(name);
	}

	/**
	 * OSGi platform creation. The chooseOsgiPlatform method is called to
	 * determine what platform will be used by the test - if an invalid/null
	 * String is returned, Equinox will be used by default.
	 * 
	 * @return the OSGi platform
	 */
	protected OsgiPlatform createPlatform() {
		String platformName = getPlatformName();
		if (platformName != null) {
			platformName = platformName.toLowerCase();

			if (platformName.contains(FELIX_PLATFORM)) {
				return new FelixPlatform();

			}
			if (platformName.contains(KNOPFLERFISH_PLATFORM)) {
				return new KnopflerfishPlatform();
			}
		}

		return new EquinoxPlatform();
	}

	/**
	 * Indicate what OSGi platform to be used by the test suite. By default, the
	 * 'spring.osgi.test.framework' is used.
	 * 
	 * @return platform
	 */
	protected String getPlatformName() {
		String systemProperty = System.getProperty(OSGI_FRAMEWORK_SELECTOR);

		return (systemProperty == null ? EQUINOX_PLATFORM : systemProperty);
	}

	/**
	 * Logs the underlying OSGi information (which can be tricky).
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
		log.info(platformInfo + " started");
	}

	/**
	 * Return the resource loader used by this test.
	 * 
	 * @return an OsgiBundleResourceLoader if the bundleContext was set or null
	 * otherwise.
	 */
	protected ResourceLoader getResourceLoader() {
		return resourceLoader;
	}

	private Resource[] createResources(String[] bundles) {
		Resource[] res = new Resource[bundles.length];
		ResourceLoader loader = new DefaultResourceLoader();

		for (int i = 0; i < bundles.length; i++) {
			res[i] = loader.getResource(bundles[i]);
		}
		return res;
	}

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
	 * Customized setUp - the OSGi platform will be started (if needed) and
	 * cached for the test suite execution.
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected final void setUp() throws Exception {

		if (getName() == null)
			throw new IllegalArgumentException("no test specified");

		// write the testname into the System properties
		System.getProperties().put(OsgiJUnitTest.OSGI_TEST, getClass().getName());

		// create streams first to avoid deadlocks in setting up the stream on
		// the OSGi side
		setupStreams();

		// start OSGi platform (the caching is done inside the method).
		try {
			startup();
		}
		catch (Exception e) {
			log.debug("Caught exception starting up", e);
			throw e;
		}

		log.debug("writing test name to stream:" + getName());
		// write test name to OSGi
		outputStream.writeUTF(getName());
		outputStream.flush();

		// invoke OSGi test run
		invokeOSGiTestExecution();

	}

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

            // add bootDelegation packages
            System.setProperty(ORG_OSGI_FRAMEWORK_BOOTDELEGATION, getBootDelegationPackageString());

            osgiPlatform = createPlatform();
            // start platform
			log.debug("about to start " + osgiPlatform);
			osgiPlatform.start();
			// platform context
			platformContext = osgiPlatform.getBundleContext();

			// log platform name and version
			logPlatformInfo(platformContext);

			// merge bundles
			String[] mandatoryBundles = getMandatoryBundles();
			String[] optionalBundles = getBundleLocations();

			String[] allBundles = new String[mandatoryBundles.length + optionalBundles.length];
			System.arraycopy(mandatoryBundles, 0, allBundles, 0, mandatoryBundles.length);
			System.arraycopy(optionalBundles, 0, allBundles, mandatoryBundles.length, optionalBundles.length);

			// install bundles (from the local system/classpath)
			Resource[] bundleResources = createResources(allBundles);

			// initializeLog4J();

			Bundle[] bundles = new Bundle[bundleResources.length];
			for (int i = 0; i < bundleResources.length; i++) {
				bundles[i] = installBundle(bundleResources[i]);
			}

			// start bundles
			for (int i = 0; i < bundles.length; i++) {
				log.debug("starting bundle " + bundles[i].getBundleId() + "[" + bundles[i].getSymbolicName() + "]");
				try {
					bundles[i].start();
				}
				catch (Throwable ex) {
					log.warn("can't start bundle " + bundles[i].getBundleId() + "[" + bundles[i].getSymbolicName()
							+ "]", ex);
				}

			}
			postProcessBundleContext(platformContext);

			// get JUnit test service reference
			// this is a loose reference - update it if the Activator class is
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

			onSetUpBeforeOsgi();

		}
	}

    /**
     * Return a String representation for the boot delegation packages list.
     *
     * @return
     */
    private String getBootDelegationPackageString() {
        StringBuffer buf = new StringBuffer();

        for (Iterator iter = getBootDelegationPackages().iterator(); iter.hasNext();) {
            buf.append(((String) iter.next()).trim());
            if (iter.hasNext()) {
                buf.append(",");
            }
        }

        return buf.toString();
    }


    /**
     *
     * @return the list of packages the OSGi platform will delegate to the boot class path
     *         Answer an empty list if none.
     */
    protected List getBootDelegationPackages() {
        List defaults = new ArrayList();
        defaults.add("javax.*");
        defaults.add("org.w3c.*");
        defaults.add("sun.*");
        defaults.add("org.xml.*");
        defaults.add("com.sun.*");
        return defaults;
    }

    /**
	 * Callback for processing the bundle context after the bundles have been
	 * installed and started.
	 * 
	 * @param context
	 */
	protected void postProcessBundleContext(BundleContext context) throws Exception {
	}

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
		log.debug("installing bundle from location " + location.getDescription());
		return platformContext.installBundle(location.getDescription(), location.getInputStream());
	}

	/**
	 * Hook for adding behavior after the OSGi platform has been started (is
	 * this really needed).
	 * 
	 */
	protected void onSetUpBeforeOsgi() {
	}

	/**
	 * the setUp version for the OSGi environment.
	 * 
	 * @throws Exception
	 */
	public void onSetUp() throws Exception {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	public final void tearDown() throws Exception {
		cleanupStreams();
		onTearDown();
	}

	public void onTearDown() throws Exception {
	}

	private void readTestResult() {
		// finish stream creation (to avoid circullar dependencies)
		createInStream();

		log.debug("reading OSGi results for test [" + getName() + "]");

		TestUtils.receiveTestResult(this.originalResult, this, inputStream);

		log.debug("test[" + getName() + "]'s result read");
	}

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

	private void shutdownTest() {
		cleanupStreams();

		log.info("shutting down OSGi platform");
		if (osgiPlatform != null) {
			try {
				osgiPlatform.stop();
			}
			catch (Exception ex) {
				// swallow
				log.warn("shutdown procedure threw exception " + ex);
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
		// flush header write away
		outputStream.flush();

		log.debug("OSGi streams setup");
	}

	private void cleanupStreams() {
		try {
			if (inputStream != null)
				inputStream.close();
		}
		catch (IOException e) {
			// swallow
		}

		try {
			if (outputStream != null)
				outputStream.close();
		}
		catch (IOException e) {
			// swallow
		}
	}

	private void createInStream() {
		try {
			byte[] inputSource = (byte[]) System.getProperties().get(OsgiJUnitTest.FROM_OSGI);
			inputStream = new ObjectInputStream(new ByteArrayInputStream(inputSource));
		}
		catch (IOException ex) {
			throw new RuntimeException("cannot open streams " + ex);
		}

	}

	/**
	 * Change the normal test execution by adding result retrieval from OSGi
	 * realm.
	 * 
	 * @see junit.framework.TestCase#run()
	 */
	public final void runTest() throws Throwable {
		readTestResult();
	}

	/**
	 * Actual test execution (delegates to the TestCase implementation).
	 * 
	 * @throws Throwable
	 */
	public final void osgiRunTest() throws Throwable {
		super.runTest();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#run(junit.framework.TestResult)
	 */
	public final void run(TestResult result) {
		// get a hold of the test result
		this.originalResult = result;
		super.run(result);
	}

	/**
	 * Set the bundle context to be used by this test. This method is called
	 * automatically by the test infrastructure after the OSGi platform is being
	 * setup.
	 */
	public final void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		// instantiate ResourceLoader
		this.resourceLoader = new OsgiBundleResourceLoader(bundleContext.getBundle());
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public void waitOnContextCreation(String forBundleWithSymbolicName, long timeout, TimeUnit unit) {
		// use a barrier to ensure we don't proceed until context is published
		final CyclicBarrier barrier = new CyclicBarrier(2);

		Thread waitThread = new Thread(new ApplicationContextWaiter(barrier, bundleContext, forBundleWithSymbolicName));
		waitThread.start();

		try {
			barrier.await(timeout, unit);
		}
		catch (Throwable e) {
            // dumpStacks();
            throw new RuntimeException("Gave up waiting for application context for '" + forBundleWithSymbolicName
					+ "' to be created");
		}
	}

	public void waitOnContextCreation(String forBundleWithSymbolicName) {
        waitOnContextCreation(forBundleWithSymbolicName, 5L, TimeUnit.SECONDS);
    }

	public Bundle findBundleByLocation(String bundleLocation) {
		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getLocation().equals(bundleLocation)) {
				return bundles[i];
			}
		}
		return null;
	}

	public Bundle findBundleBySymbolicName(String sybmolicName) {
		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getSymbolicName().equals(sybmolicName)) {
				return bundles[i];
			}
		}
		return null;
	}
   /*
	public void dumpStacks() {
		Map stacks = Thread.getAllStackTraces();
		for (Iterator i = stacks.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry)i.next();
			System.out.println("");
			System.out.println(e.getKey());
			StackTraceElement[] se = (StackTraceElement[])e.getValue();
			for (int j=0; j<se.length; j++) {
				System.out.println(" " + se[j]);
			}
		}
	}
   */
	private static class ApplicationContextWaiter implements Runnable, ServiceListener {

		private final String symbolicName;

		private final CyclicBarrier barrier;

		private final BundleContext context;

		public ApplicationContextWaiter(CyclicBarrier barrier, BundleContext context, String bundleSymbolicName) {
			this.symbolicName = bundleSymbolicName;
			this.barrier = barrier;
			this.context = context;
		}

		public void run() {
			String filter = "(org.springframework.context.service.name=" + symbolicName + ")";
			try {
				context.addServiceListener(this, filter);
				// now look and see if the service was already registered before
				// we even got here...
				if (this.context.getServiceReferences("org.springframework.context.ApplicationContext", filter) != null) {
					returnControl();
				}
			}
			catch (InvalidSyntaxException badSyntaxEx) {
				throw new IllegalStateException("OSGi runtime rejected filter '" + filter + "'");
			}
		}

		public void serviceChanged(ServiceEvent event) {
			if (event.getType() == ServiceEvent.REGISTERED) {
				// our wait is over...
				returnControl();
			}
		}

		private void returnControl() {
			this.context.removeServiceListener(this);
			try {
				this.barrier.await();
			}
			catch (BrokenBarrierException ex) {
				// return;
			}
			catch (InterruptedException intEx) {
				// return;
			}
		}
	}
}
