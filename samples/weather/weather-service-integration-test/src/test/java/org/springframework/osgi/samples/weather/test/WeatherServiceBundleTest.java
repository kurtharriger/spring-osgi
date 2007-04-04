/*
 * Copyright 2006 the original author or authors.
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
package org.springframework.osgi.samples.weather.test;

import java.util.GregorianCalendar;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.samples.weather.service.WeatherService;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;

/**
 * Starts up an OSGi environment (equinox, knopflerfish, or
 * felix according to the profile selected) and installs the
 * weather service bundle and the bundles it depends on. The test
 * classes in this project will be turned into a virtual bundle
 * which is also installed and the tests are then run inside the
 * OSGi runtime.
 * <p/>
 * The tests have access to a BundleContext, which we use to
 * test that the weather bean was indeed published as
 * an OSGi service.
 *
 * @author Adrian Colyer
 * @author Andy Piper
 */
public class WeatherServiceBundleTest extends AbstractConfigurableBundleCreatorTests {

    /**
     * The manifest to use for the "virtual bundle" created
     * out of the test classes and resources in this project
     */
    protected String getManifestLocation() {
        return "classpath:org/springframework/osgi/samples/weather/test/MANIFEST.MF";
    }

    /**
     * The location of the packaged OSGi bundles to be installed
     * for this test. Values are Spring resource paths. The bundles
     * we want to use are part of the same multi-project maven
     * build as this project is. Hence we use the localMavenArtifact
     * helper method to find the bundles produced by the package
     * phase of the maven build (these tests will run after the
     * packaging phase, in the integration-test phase).
     * <p/>
     * JUnit, commons-logging, spring-core and the spring OSGi
     * test bundle are automatically included so do not need
     * to be specified here.
     */
    protected String[] getBundles() {
        return new String[]{
			localMavenArtifact("org.springframework.osgi", "spring-jmx", "2.1-SNAPSHOT"),            
            localMavenArtifact("org.springframework.osgi", "wiring-bundle", "1.0-m1")
        };
    }

    /**
     * The superclass provides us access to the root bundle
     * context via the 'getBundleContext' operation
     */
    public void testOSGiStartedOk() {
        BundleContext bundleContext = getBundleContext();
        assertNotNull(bundleContext);
    }

    /**
     * The simple service should have been exported as an
     * OSGi service, which we can verify using the OSGi
     * service APIs.
     * <p/>
     * In a Spring bundle, using osgi:reference is a much
     * easier way to get a reference to a published service.
     */
    public void testWeatherServiceExported() {
        waitOnContextCreation("org.springframework.osgi.samples.weather.service");
        waitOnContextCreation("org.springframework.osgi.samples.wiring.bundle");

        BundleContext context = getBundleContext();
        ServiceReference ref = context.getServiceReference("org.springframework.osgi.samples.weather.service.WeatherService");
        assertNotNull("Service Reference is null", ref);
        try {
            WeatherService weather = (WeatherService) context.getService(ref);
            assertNotNull("Cannot find the service", weather);
            assertEquals(new Double(15.0), weather.getHistoricalHigh(new GregorianCalendar(2004, 0, 1).getTime()));
        }
        finally {
            context.ungetService(ref);
        }
    }
}
