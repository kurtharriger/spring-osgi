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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;

/**
 * JUnit contract for OSGi environments.
 * 
 * @author Costin Leau
 * 
 */
public interface OsgiJUnitTest {

    /**
     * Lookup marker for "to-OSGi" communication channel.
     */
    public static final String FOR_OSGI = OsgiJUnitTest.class.getName() + "-osgi[in]";
    /**
     * Lookup marker for "from-OSGi" communication channel.
     */
    public static final String FROM_OSGI = OsgiJUnitTest.class.getName() + "-osgi[out]";

    /**
     * Lookup marker for the test suite that is executed inside the OSGi
     * container.
     */
    public static final String OSGI_TEST = OsgiJUnitTest.class.getName() + "-test";

    /**
     * Replacement for the 'traditional' setUp.
     *
     * @see junit.framework.TestCase#setUp
     * @throws Exception
     */
    public void onSetUp() throws Exception;

    /**
     * Replacement for the 'traditional' tearDown.
     *
     * @see junit.framework.TestCase#tearDown
     * @throws Exception
     */
    public void onTearDown() throws Exception;

    /**
     * Replacement for the 'traditional' runTest.
     * @throws Throwable
     */
    public void osgiRunTest() throws Throwable;

    public void setName(String name);


    /**
     * Provides the OSGi bundle context to the test
     * @param bundleContext
     */
    public void setBundleContext(BundleContext bundleContext);


    /**
     * Find a bundle by the bundle's location.
     * @return the bundle matching the location or null if not found
     */
    public Bundle findBundleByLocation(String bundleLocation);


    /**
     * Find a bundle by the bundle's symbolic name
     * @return the bundle matching the symbolic name or null if not found
     */
    public Bundle findBundleBySymbolicName(String bundleSymbolicName);
}
