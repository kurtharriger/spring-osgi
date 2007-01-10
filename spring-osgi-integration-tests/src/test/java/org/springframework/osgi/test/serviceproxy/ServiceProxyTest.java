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
package org.springframework.osgi.test.serviceproxy;

import java.util.Date;

import org.aopalliance.aop.Advice;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.osgi.service.NoSuchServiceException;
import org.springframework.osgi.service.support.ClassTargetSource;
import org.springframework.osgi.service.support.cardinality.OsgiSingleServiceInterceptor;
import org.springframework.osgi.test.ConfigurableBundleCreatorTests;
import org.springframework.util.ClassUtils;

/**
 * @author Costin Leau
 * 
 */
public class ServiceProxyTest extends ConfigurableBundleCreatorTests {

	protected String[] getBundleLocations() {
		return new String[] { localMavenArtifact("org.springframework.osgi", "aopalliance.osgi", "1.0-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "cglib-nodep.osgi", "2.1.3-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-aop", "2.1-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-beans", "2.1-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-context", "2.1-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-osgi-core", "1.0-SNAPSHOT"),
				localMavenArtifact("org.springframework.osgi", "spring-osgi-extender", "1.0-SNAPSHOT") };
	}

	protected String getManifestLocation() {
		return "org/springframework/osgi/test/serviceproxy/ServiceProxyTest.MF";
	}

	private ServiceRegistration publishService(Object obj) throws Exception {
		return getBundleContext().registerService(obj.getClass().getName(), obj, null);
	}

	private Object createProxy(final Class clazz, Advice cardinalityInterceptor) {
		ProxyFactory factory = new ProxyFactory();
		factory.setProxyTargetClass(true);
		factory.setOptimize(true);
		factory.setTargetSource(new ClassTargetSource(clazz));

		factory.addAdvice(cardinalityInterceptor);
		factory.setFrozen(true);

		return factory.getProxy(ProxyFactory.class.getClassLoader());
	}

	
	private Advice createCardinalityAdvice(Class clazz, boolean mandatoryEnd) {
		OsgiSingleServiceInterceptor interceptor = new OsgiSingleServiceInterceptor(mandatoryEnd);
		interceptor.setClass(clazz.getName());
		interceptor.setContext(getBundleContext());
		// fast retry
		interceptor.getRetryTemplate().setWaitTime(1);
		return interceptor;

	}

	public void testCglibLibraryVisibility() {
		// check visibility on spring-core jar
		// note that cglib is not declared inside this bundle but should be seen
		// by spring-core (which contains the util classes)
		assertTrue(ClassUtils.isPresent("net.sf.cglib.proxy.Enhancer"));
	}

	public void testMandatoryEndProxy() throws Exception {
		long time = 123456;
		Date date = new Date(time);
		ServiceRegistration reg = publishService(date);
		BundleContext ctx = getBundleContext();

		try {
			ServiceReference ref = ctx.getServiceReference(Date.class.getName());
			assertNotNull(ref);
			Date proxy = (Date) createProxy(Date.class, createCardinalityAdvice(Date.class, true));
			assertEquals(time, proxy.getTime());
			// take down service
			reg.unregister();
			// reference is invalid
			assertNull(ref.getBundle());

			try {
				proxy.getTime();
				fail("should have thrown exception");
			}
			catch (NoSuchServiceException nsse) {
				// service failed
			}

			// rebind the service
			reg = publishService(date);
			// retest the service
			assertEquals(time, proxy.getTime());
		}
		finally {
			if (reg != null)
				try {
					reg.unregister();
				}
				catch (Exception ex) {
					// ignore
				}
		}
	}

	public void testOptionalEndProxy() throws Exception {
		long time = 123456;
		Date date = new Date(time);
		ServiceRegistration reg = publishService(date);
		BundleContext ctx = getBundleContext();

		try {
			ServiceReference ref = ctx.getServiceReference(Date.class.getName());
			assertNotNull(ref);
			Date proxy = (Date) createProxy(Date.class, createCardinalityAdvice(Date.class, false));
			assertEquals(time, proxy.getTime());
			// take down service
			reg.unregister();
			// reference is invalid
			assertNull(ref.getBundle());

			try {
				// will return default value
				assertEquals(0, proxy.getTime());
			}
			catch (NoSuchServiceException nsse) {
				fail("should have returned null and not throw an exception");
			}

			// rebind the service
			reg = publishService(date);
			// retest the service
			assertEquals(time, proxy.getTime());
		}
		finally {
			if (reg != null)
				try {
					reg.unregister();
				}
				catch (Exception ex) {
					// ignore
				}
		}
	}
}
