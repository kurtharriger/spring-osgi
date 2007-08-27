package org.springframework.osgi.iandt.extender;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;

/**
 * @author Hal Hildebrand
 *         Date: May 21, 2007
 *         Time: 4:43:52 PM
 */
public class ExtenderTest extends AbstractConfigurableBundleCreatorTests {

    protected String getManifestLocation() {
        return null;
    }


    // Overridden to remove the spring extender bundle!
    protected String[] getMandatoryBundles() {
        return new String[]{getSlf4jApi(), getJclOverSlf4jUrl(), getSlf4jLog4jUrl(), getLog4jLibUrl(),
                            getJUnitLibUrl(), getSpringCoreBundleUrl(), getSpringBeansUrl(), getSpringContextUrl(),
                            getSpringMockUrl(), getAopAllianceUrl(), getAsmLibrary(),
                            getSpringAopUrl(), getUtilConcurrentLibUrl(),
                            getSpringOSGiIoBundleUrl(), getSpringOSGiCoreBundleUrl(), getSpringOSGiTestBundleUrl()};
    }


    // Specifically cannot wait - test scenario has bundles which are spring powered, but will not be started.
    protected boolean shouldWaitForSpringBundlesContextCreation() {
        return false;
    }


    protected String[] getBundles() {
        return new String[]{
                localMavenArtifact("org.springframework.osgi", "commons-collections.osgi", "3.2-SNAPSHOT"),
                localMavenArtifact("org.springframework.osgi", "org.springframework.osgi.iandt.lifecycle",
                                   getSpringOsgiVersion())
        };
    }


    public void testLifecycle() throws Exception {
        assertNull("Guinea pig has already been started",
                   System.getProperty("org.springframework.osgi.iandt.lifecycle.GuineaPig.close"));

        StringBuffer filter = new StringBuffer();
        filter.append("(&");
        filter.append("(").append(Constants.OBJECTCLASS).append("=")
                .append(AbstractRefreshableApplicationContext.class.getName()).append(")");
        filter.append("(")
                .append("org.springframework.context.service.name");
        filter.append("=").append("org.springframework.osgi.iandt.lifecycle").append(")");
        filter.append(")");
        ServiceTracker tracker = new ServiceTracker(getBundleContext(),
                                                    getBundleContext().createFilter(filter.toString()),
                                                    null);
        tracker.open();

        AbstractRefreshableApplicationContext appContext = (AbstractRefreshableApplicationContext) tracker
                .waitForService(1);

        assertNull("lifecycle application context does not exist", appContext);


        Resource extenderResource = getLocator()
                .locateArtifact("org.springframework.osgi", "spring-osgi-extender", getSpringOsgiVersion());
        assertNotNull("Extender bundle resource", extenderResource);
        Bundle extenderBundle = getBundleContext().installBundle(extenderResource.getURL().toExternalForm());
        assertNotNull("Extender bundle", extenderBundle);

        extenderBundle.start();

        tracker.open();

        appContext = (AbstractRefreshableApplicationContext) tracker
                .waitForService(60000);

        assertNotNull("lifecycle application context exists", appContext);


        assertNotSame("Guinea pig hasn't already been shutdown", "true",
                      System.getProperty("org.springframework.osgi.iandt.lifecycle.GuineaPig.close"));

        assertEquals("Guinea pig started up", "true",
                     System.getProperty("org.springframework.osgi.iandt.lifecycle.GuineaPig.startUp"));

    }
}
