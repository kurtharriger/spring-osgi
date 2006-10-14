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
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.osgi.test.platform.EquinoxPlatform;
import org.springframework.osgi.test.platform.FelixPlatform;
import org.springframework.osgi.test.platform.KnopflerfishPlatform;
import org.springframework.osgi.test.platform.OsgiPlatform;
import org.springframework.util.Assert;

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
	private static BundleContext context;
	private static Bundle[] bundles;

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

    private static final String ACTIVATOR_REFERENCE = "org.springframework.osgi.test.JUnitTestActivator";

	public static final String EQUINOX_PLATFORM = "equinox";
	public static final String KNOPFLERFISH_PLATFORM = "knopflerfish";
	public static final String FELIX_PLATFORM = "felix";

	public static final String OSGI_FRAMEWORK_SELECTOR = "org.springframework.osgi.test.framework";

	protected final Log log = LogFactory.getLog(getClass());

	private static String getSpringOSGiTestBundleUrl() {
		return localMavenArtifact("org.springframework.osgi.test", "1.0-SNAPSHOT");
	}

	private static String getSpringCoreBundleUrl() {
		return localMavenArtifact("spring-core", "2.1-SNAPSHOT");
	}

	private static String getLog4jLibUrl() {
		return localMavenArtifact("log4j.osgi", "1.2.13-SNAPSHOT");
	}

	private static String getCommonsLoggingLibUrl() {
		return localMavenArtifact("commons-logging.osgi", "1.1-SNAPSHOT");
	}
	
	private static String getJUnitLibUrl() {
		return localMavenArtifact("junit.osgi", "3.8.1-SNAPSHOT");
	}



    /**
     * Answer the url string of the indicated bundle in the local Maven repository
     *
     * @param groupId - the groupId of the organization supplying the bundle
     * @param artifact - the artifact id of the bundle
     * @param version - the version of the bundle
     * @return the String representing the URL location of this bundle
     */
    public static String localMavenBundle(String groupId, String artifact, String version) {
		File userHome = new File(System.getProperty("user.home"));
		File repositoryHome = new File(userHome, ".m2/repository");
        String location = groupId.replace('.', '/');
        location += '/';
        location += artifact;
        location += '/';
        location += version;
        location += '/';
        location += artifact;
        location += '-';
        location += version;
        location += ".jar";
        return "file:" + new File(repositoryHome, location).getAbsolutePath();
    }
    
    public static String localMavenArtifact(String artifactId, String version) {
    	try {
    		File found = new MavenPackagedArtifactFinder(artifactId,version).findPackagedArtifact(new File("."));
    		return found.toURL().toExternalForm();
    	} catch (IOException ioEx) {
    		throw new IllegalStateException(
    			"Artifact " + artifactId + "-" + version + ".jar" + 
    			" could not be found",ioEx);
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
	 * @return the array of mandatory bundle names
	 */
	private String[] getMandatoryBundles() {
		return new String[] { 
				getJUnitLibUrl(),
				getLog4jLibUrl(), 
				getCommonsLoggingLibUrl(), 
				getSpringCoreBundleUrl(), 
				getSpringOSGiTestBundleUrl() };
	}

	public AbstractOsgiTests() {
		super();
	};

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
                log.info("Creating Felix Platform");
                return new FelixPlatform();

            }
			if (platformName.contains(KNOPFLERFISH_PLATFORM)) {
                log.info("Creating Knopflerfish Platform");
				return new KnopflerfishPlatform();
            }
        }

        log.info("Creating Equinox Platform");
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

	protected ResourceLoader getResourceLoader() {
		return new DefaultResourceLoader();
	}

	private Resource[] createResources(String[] bundles) {
		Resource[] res = new Resource[bundles.length];

		for (int i = 0; i < bundles.length; i++) {
			res[i] = getResourceLoader().getResource(bundles[i]);
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
        } catch (Exception e) {
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

			log.info("initializing OSGi platform...");

			osgiPlatform = createPlatform();
			// start platform
			osgiPlatform.start();
			context = osgiPlatform.getBundleContext();
			// merge bundles
			String[] mandatoryBundles = getMandatoryBundles();
			String[] optionalBundles = getBundleLocations();

			String[] allBundles = new String[mandatoryBundles.length + optionalBundles.length];
			System.arraycopy(mandatoryBundles, 0, allBundles, 0, mandatoryBundles.length);
			System.arraycopy(optionalBundles, 0, allBundles, mandatoryBundles.length, optionalBundles.length);

			// install bundles
			Resource[] bundleResources = createResources(allBundles);

			bundles = new Bundle[bundleResources.length];
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
			postProcessBundleContext(context);

			// get JUnit test service reference
			// this is a loose reference - update it if the Activator class is
			// changed.
			ServiceReference reference = context.getServiceReference(ACTIVATOR_REFERENCE);
			if (reference == null)
				throw new IllegalStateException("no OSGi service reference found at " + ACTIVATOR_REFERENCE);

			service = context.getService(reference);
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
		Assert.notNull(context);
		Assert.notNull(location);
		log.debug("installing bundle from location " + location.getDescription());
		return context.installBundle(location.getDescription(), location.getInputStream());
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

		// 4K seems to be enough
		byte[] inArray = new byte[1024 * 4];
		byte[] outArray = new byte[1024 * 4];

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

    public final void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

}
