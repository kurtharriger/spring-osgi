/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.iandt.serviceProxyFactoryBean;

import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

import org.osgi.framework.ServiceRegistration;
import org.springframework.aop.SpringProxy;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.ServiceReferenceAware;
import org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.util.MapBasedDictionary;

/**
 * @author Costin Leau
 * 
 */
public class ServiceRefAwareWithSingleServiceTest extends ServiceBaseTest {

	private OsgiServiceProxyFactoryBean fb;

	protected void onSetUp() throws Exception {
		fb = new OsgiServiceProxyFactoryBean();
		fb.setBundleContext(bundleContext);
		// execute retries fast
		fb.setRetryTimes(1);
		fb.setTimeout(1);
		ClassLoader classLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundleContext.getBundle());
		fb.setBeanClassLoader(classLoader);
	}

	protected void onTearDown() throws Exception {
		fb = null;
	}

	public void tstProxyForUnaryCardinality() throws Exception {
		long time = 1234;
		Date date = new Date(time);
		Dictionary dict = new MapBasedDictionary();
		ServiceRegistration reg = publishService(date);

		fb = new OsgiServiceProxyFactoryBean();
		fb.setMandatory(true);

		fb.setInterface(new Class[] { Date.class });
		fb.afterPropertiesSet();

		ServiceReferenceAware refAware = null;
		try {
			Object result = fb.getObject();
			assertTrue(result instanceof Date);
			// check it's our object
			assertEquals(time, ((Date) result).getTime());
			assertTrue(result instanceof SpringProxy);
			assertTrue(result instanceof ServiceReferenceAware);

			refAware = (ServiceReferenceAware) result;
			assertNotNull(refAware.getServiceReference());
		}
		finally {
			if (reg != null)
				reg.unregister();
		}

		// test reference after the service went down
		assertNotNull(refAware.getServiceReference());
		assertNull(refAware.getServiceReference().getBundle());
	}

	public void testServiceReferenceProperties() throws Exception {
		/**
		 * this fails with following stack trace if tstProxyForUnaryCardinality
		 * actually runs. Looks like an internal issue with cglib.
		 * 
		 * Caused by: java.lang.NullPointerException at
		 * net.sf.cglib.core.AbstractClassGenerator.getClassNameCache(AbstractClassGenerator.java:80)
		 * at
		 * net.sf.cglib.core.AbstractClassGenerator.create(AbstractClassGenerator.java:218)
		 * at net.sf.cglib.proxy.Enhancer.createHelper(Enhancer.java:377) at
		 * net.sf.cglib.proxy.Enhancer.create(Enhancer.java:285) at
		 * org.springframework.aop.framework.Cglib2AopProxy.getProxy(Cglib2AopProxy.java:196)
		 * at
		 * org.springframework.aop.framework.ProxyFactory.getProxy(ProxyFactory.java:110)
		 * ...
		 */

		long time = 1234;
		Date date = new Date(time);
		Dictionary dict = new MapBasedDictionary();
		dict.put("foo", "bar");
		dict.put("george", "michael");

		ServiceRegistration reg = publishService(date, dict);

		fb.setMandatory(true);
		fb.setInterface(new Class[] { Date.class });
		fb.afterPropertiesSet();

		try {
			Object result = fb.getObject();
			assertTrue(result instanceof Date);
			// check it's our object
			assertEquals(time, ((Date) result).getTime());

			ServiceReferenceAware refAware = (ServiceReferenceAware) result;
			// get properties
			Map map = refAware.getServiceProperties();

			// compare content
			assertTrue(doesMapContainsDictionary(dict, map));

			// update properties dynamically
			dict.put("another", "property");

			reg.setProperties(dict);

			assertTrue(doesMapContainsDictionary(dict, map));
		}
		finally {
			if (reg != null)
				reg.unregister();
		}
	}

	/**
	 * Check if the 'test' map contains the original Dictionary.
	 * 
	 * @param original
	 * @param test
	 * @return
	 */
	private boolean doesMapContainsDictionary(Dictionary original, Map test) {
		Enumeration enm = original.keys();
		while (enm.hasMoreElements()) {
			if (!test.containsKey(enm.nextElement()))
				return false;
		}

		return true;
	}

}
