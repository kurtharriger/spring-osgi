package org.springframework.osgi.iandt.error;

import org.springframework.core.io.Resource;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.osgi.framework.Bundle;

/**
 * @author Hal Hildebrand
 *         Date: May 29, 2007
 *         Time: 5:07:01 PM
 */
public class ErrorHandlingTest extends AbstractConfigurableBundleCreatorTests {

    protected String getManifestLocation() {
        return null;
    }


    // Specifically do not wait
    protected boolean shouldWaitForSpringBundlesContextCreation() {
        return false;
    }


    /**
     * While it may appear that this test is doing nothing, what it is doing is testing what
     * happens when the OSGi framework is shutdown while the Spring/OSGi extender is still
     * running threads which are trying to clean up after an error condition in creating the
     * context.  When running under the Felix platform, this test will produce deadlock or,
     * if things are really going south, serious CDNFE when handling the error (e.g.
     * java.lang.InputStream, java.lang.Integer, etc.).  The reason for the
     * latter is that the OSGi framework is shutdown and the class loaders being used by
     * extender threads are no longer valid.  The deadlock case stems from a similar condition
     * in that the system is deadlocked around the synchronization of the context.create()/close()
     * because the underlying framework is deadlocked around resource resolution.  As we process
     * the close of the context synchronously, we're blocked waiting for the resource resolution which
     * is blocked because the framework is shutting down.  We can't unblock because we're still waiting
     * for the lock on the context to shutdown and unblock the event notification from the underlying
     * framework.
     *
     * Consequently, please do not remove or modify this test unless you talk to Hal.  :) 
     */
    public void testErrorHandling() throws Exception {
        Resource errorResource = getLocator()
                .locateArtifact("org.springframework.osgi", "org.springframework.osgi.iandt.error",
                                getSpringOsgiVersion());
        assertNotNull("Error bundle resource exists", errorResource);
        Bundle errorBundle = getBundleContext().installBundle(errorResource.getURL().toExternalForm());
        assertNotNull("Errro bundle exists", errorBundle);

        errorBundle.start(); 
    }
}