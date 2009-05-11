/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.iandt.r41.lazy;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.core.io.Resource;
import org.springframework.osgi.iandt.BaseIntegrationTest;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Base test for OSGi R4.1 features (mainly lazy bootstrapping).
 * 
 * @author Costin Leau
 */
public abstract class BaseR41IntegrationTest extends BaseIntegrationTest {

	private static final Method START_LAZY_METHOD;
	public static final Integer START_ACTIVATION_POLICY = Integer.valueOf(0x00000002);

	static {
		Method m = null;
		try {
			m = Bundle.class.getMethod("start", new Class[] { int.class });
		}
		catch (Exception ex) {
		}

		START_LAZY_METHOD = m;
	}


	@Override
	protected boolean isDisabledInThisEnvironment(String testMethodName) {
		return (START_LAZY_METHOD == null || !getPlatformName().contains("Equinox"));
	}

	protected String[] getBundleContentPattern() {
		String pkg = getClass().getPackage().getName().replace('.', '/').concat("/");
		String[] patterns = new String[] { BaseR41IntegrationTest.class.getName().replace('.', '/').concat("*.class"),
			BaseIntegrationTest.class.getName().replace('.', '/').concat("*.class"), pkg + "**/*" };
		return patterns;
	}

	protected void startBundleLazy(Bundle bundle) {
		if (bundle != null) {
			ReflectionUtils.invokeMethod(START_LAZY_METHOD, bundle, START_ACTIVATION_POLICY);
		}
	}

	protected abstract String[] lazyBundles();

	@Override
	protected void postProcessBundleContext(BundleContext context) throws Exception {
		super.postProcessBundleContext(context);

		Resource[] res = locateBundles(lazyBundles());
		for (Resource resource : res) {
			Bundle bundle = context.installBundle(resource.getDescription(), resource.getInputStream());
			startBundleLazy(bundle);
			assertTrue("the lazy bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + " has been activated",
				OsgiBundleUtils.isBundleLazyActivated(bundle));
		}
	}

	protected ServiceReference getServiceReference(final String filter) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);

		ServiceListener sl = new ServiceListener() {

			public void serviceChanged(ServiceEvent event) {
				latch.countDown();
			}
		};

		bundleContext.addServiceListener(sl, filter);
		if (OsgiServiceReferenceUtils.getServiceReference(bundleContext, filter) != null) {
			latch.countDown();
		}
		try {
			latch.await(2, TimeUnit.MINUTES);
		}
		finally {
			bundleContext.removeServiceListener(sl);
		}
		return OsgiServiceReferenceUtils.getServiceReference(bundleContext, filter);
	}
}