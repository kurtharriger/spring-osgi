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
package org.springframework.osgi.iandt.lifecycle;

import org.osgi.framework.Bundle;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;

/**
 * @author Hal Hildebrand
 *         Date: Oct 15, 2006
 *         Time: 5:51:36 PM
 */
public class LifecycleTest extends AbstractConfigurableBundleCreatorTests {

    protected String getManifestLocation() {
        return "classpath:org/springframework/osgi/iandt/lifecycle/MANIFEST.MF";
    }

    protected String[] getBundles() {
        return new String[]{
                localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT"), 
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.iandt.lifecycle", "1.0-m2-SNAPSHOT")
        };
    }
    
    

	public void testLifecycle() throws Exception {
    	waitOnContextCreation("org.springframework.osgi.iandt.lifecycle");
        assertNotSame("Guinea pig has already been shutdown", "true",
                      System.getProperty("org.springframework.osgi.iandt.lifecycle.GuineaPig.close"));

        assertEquals("Guinea pig didn't startup", "true",
                     System.getProperty("org.springframework.osgi.iandt.lifecycle.GuineaPig.startUp"));
        Bundle[] bundles = getBundleContext().getBundles();
        Bundle testBundle = null;
        for (int i = 0; i < bundles.length; i++) {
            if ("org.springframework.osgi.iandt.lifecycle".equals(bundles[i].getSymbolicName())) {
                testBundle = bundles[i];
                break;
            }
        }
        assertNotNull("Could not find the test bundle", testBundle);
        testBundle.stop();
	    while (testBundle.getState() == Bundle.STOPPING) {
            Thread.sleep(10);
        }
        assertEquals("Guinea pig didn't shutdown", "true",
                     System.getProperty("org.springframework.osgi.iandt.lifecycle.GuineaPig.close"));
    }
}
