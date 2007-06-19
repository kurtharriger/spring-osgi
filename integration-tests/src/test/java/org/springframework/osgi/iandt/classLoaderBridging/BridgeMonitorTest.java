package org.springframework.osgi.iandt.classLoaderBridging;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.collection.OsgiServiceCollection;
import org.springframework.osgi.service.importer.ReferenceClassLoadingOptions;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.util.ClassUtils;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * @author Hal Hildebrand
 *         Date: Jun 19, 2007
 *         Time: 6:40:36 PM
 */
public class BridgeMonitorTest extends AbstractConfigurableBundleCreatorTests {
    static {
        System.getProperties().put("BundleDelegatingClassLoader.monitor", "org.springframework.aop.IntroductionInfo");
    }


    protected String[] getBundles() {
        return new String[]{localMavenArtifact("org.springframework.osgi", "cglib-nodep.osgi", "2.1.3-SNAPSHOT")};
    }


    protected String getManifestLocation() {
        // return
        // "org/springframework/osgi/test/serviceproxy/ServiceCollectionTest.MF";
        return null;
    }


    protected ServiceRegistration publishService(Object obj) throws Exception {
        return getBundleContext().registerService(obj.getClass().getName(), obj, null);
    }


    public void testCGLIBAvailable() throws Exception {
        assertTrue(ClassUtils.isPresent("net.sf.cglib.proxy.Enhancer", DefaultAopProxyFactory.class.getClassLoader()));
    }


    protected Collection createCollection() {
        BundleContext bundleContext = getBundleContext();
        BundleDelegatingClassLoader classLoader = BundleDelegatingClassLoader
                .createBundleClassLoaderFor(bundleContext.getBundle());
        OsgiServiceCollection collection = new OsgiServiceCollection(null, bundleContext, classLoader);
        collection.setContextClassLoader(ReferenceClassLoadingOptions.UNMANAGED);
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            collection.afterPropertiesSet();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        return collection;
    }


    public void testCollectionListener() throws Exception {
        Collection collection = createCollection();

        ServiceReference[] refs = getBundleContext().getServiceReferences(null, null);

        assertEquals(refs.length, collection.size());
        int size = collection.size();
        // register a service
        long time = 123456;
        Date date = new Date(time);
        ServiceRegistration reg = publishService(date);
        try {
            assertEquals(size + 1, collection.size());
        }
        finally {
            reg.unregister();
        }

        assertEquals(size, collection.size());
    }


    public void testCollectionContent() throws Exception {
        Collection collection = createCollection();
        ServiceReference[] refs = getBundleContext().getServiceReferences(null, null);

        assertEquals(refs.length, collection.size());
        int size = collection.size();

        // register a service
        long time = 123456;
        Date date = new Date(time);
        ServiceRegistration reg = publishService(date);
        try {
            assertEquals(size + 1, collection.size());
            // test service
            Iterator iter = collection.iterator();
            // reach our new service index
            for (int i = 0; i < size; i++) {
                iter.next();
            }
            Object myService = iter.next();
            // be sure to use classes loaded by the same CL
            assertTrue(myService instanceof Date);
            assertEquals(time, ((Date) myService).getTime());
        }
        finally {
            reg.unregister();
        }

        assertEquals(size, collection.size());
    }

}