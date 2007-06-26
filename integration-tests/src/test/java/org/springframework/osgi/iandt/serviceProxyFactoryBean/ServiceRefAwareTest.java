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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceRegistration;
import org.springframework.aop.SpringProxy;
import org.springframework.osgi.service.ServiceReferenceAware;
import org.springframework.osgi.util.MapBasedDictionary;

/**
 * Integration test for ServiceFactoryAware interface.
 * 
 * @author Costin Leau
 * 
 */
public class ServiceRefAwareTest extends ServiceBaseTest {
 
    public void tstProxyForUnaryCardinality() throws Exception {
        long time = 1234;
		Date date = new Date(time);
		Dictionary dict = new MapBasedDictionary();
		ServiceRegistration reg = publishService(date);

		fb.setCardinality("1..1");
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
         * this fails with following stack trace if tstProxyForUnaryCardinality actually runs.
         * Looks like an internal issue with cglib.
         *
         * Caused by: java.lang.NullPointerException
         at net.sf.cglib.core.AbstractClassGenerator.getClassNameCache(AbstractClassGenerator.java:80)
         at net.sf.cglib.core.AbstractClassGenerator.create(AbstractClassGenerator.java:218)
         at net.sf.cglib.proxy.Enhancer.createHelper(Enhancer.java:377)
         at net.sf.cglib.proxy.Enhancer.create(Enhancer.java:285)
         at org.springframework.aop.framework.Cglib2AopProxy.getProxy(Cglib2AopProxy.java:196)
         at org.springframework.aop.framework.ProxyFactory.getProxy(ProxyFactory.java:110)
         at org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean.createSingleServiceProxy(OsgiServiceProxyFactoryBean.java:260)
         at org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean.getObject(OsgiServiceProxyFactoryBean.java:120)
         at org.springframework.osgi.iandt.serviceProxyFactoryBean.ServiceRefAwareTest.testServiceReferenceProperties(ServiceRefAwareTest.java:87)
         at org.springframework.osgi.iandt.serviceProxyFactoryBean.ServiceRefAwareTest.testServiceReferenceProperties(ServiceRefAwareTest.java:87)
         at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
         at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
         at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
         at java.lang.reflect.Method.invoke(Method.java:585)
         at junit.framework.TestCase.runTest(TestCase.java:154)
         at org.springframework.osgi.test.AbstractOsgiTests.osgiRunTest(AbstractOsgiTests.java:275)
         at org.springframework.osgi.test.support.OsgiJUnitService$1.protect(OsgiJUnitService.java:135)
         at junit.framework.TestResult.runProtected(TestResult.java:124)
         at org.springframework.osgi.test.support.OsgiJUnitService.runTest(OsgiJUnitService.java:132)
         at org.springframework.osgi.test.support.OsgiJUnitService.executeTest(OsgiJUnitService.java:103)
         at org.springframework.osgi.test.support.OsgiJUnitService.runTest(OsgiJUnitService.java:80)
         at org.springframework.osgi.test.JUnitTestActivator.executeTest(JUnitTestActivator.java:64)
         at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
         at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
         at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
         at java.lang.reflect.Method.invoke(Method.java:585)
         at org.springframework.osgi.test.AbstractOsgiTests.invokeOSGiTestExecution(AbstractOsgiTests.java:322)
         at org.springframework.osgi.test.AbstractOsgiTests.runBare(AbstractOsgiTests.java:241)
         at org.springframework.osgi.test.AbstractOsgiTests$1.protect(AbstractOsgiTests.java:217)
         at junit.framework.TestResult.runProtected(TestResult.java:124)
         at org.springframework.osgi.test.AbstractOsgiTests.run(AbstractOsgiTests.java:215)
         at junit.framework.TestSuite.runTest(TestSuite.java:208)
         at junit.framework.TestSuite.run(TestSuite.java:203)
         at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
         at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
         at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
         at java.lang.reflect.Method.invoke(Method.java:585)
         at org.apache.maven.surefire.junit.JUnitTestSet.execute(JUnitTestSet.java:213)
         at org.apache.maven.surefire.suite.AbstractDirectoryTestSuite.executeTestSet(AbstractDirectoryTestSuite.java:138)
         at org.apache.maven.surefire.suite.AbstractDirectoryTestSuite.execute(AbstractDirectoryTestSuite.java:163)
         at org.apache.maven.surefire.Surefire.run(Surefire.java:84)
         at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
         at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
         at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
         at java.lang.reflect.Method.invoke(Method.java:585)
         at org.apache.maven.surefire.booter.SurefireBooter.runSuitesInProcess(SurefireBooter.java:244)
         at org.apache.maven.surefire.booter.SurefireBooter.main(SurefireBooter.java:814)
         */

		long time = 1234;
		Date date = new Date(time);
		Dictionary dict = new MapBasedDictionary();
		dict.put("foo", "bar");
		dict.put("george", "michael");

		ServiceRegistration reg = publishService(date, dict);

		fb.setCardinality("1..1");
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

	// this fails due to some CGLIB problems
	public void tstProxyForMultipleCardinality() throws Exception {
		fb.setCardinality("0..N");
		fb.setInterface(new Class[] { Date.class });
		fb.afterPropertiesSet();

		List registrations = new ArrayList(3);

		long time = 321;
		Date date = new Date(time);

		try {
			Object result = fb.getObject();
			assertTrue(result instanceof Collection);
			Collection col = (Collection) result;

			assertTrue(col.isEmpty());
			Iterator iter = col.iterator();

			assertFalse(iter.hasNext());
			registrations.add(publishService(date));
			Object service = iter.next();
			assertTrue(service instanceof Date);
			assertEquals(time, ((Date) service).getTime());

			assertTrue(service instanceof ServiceReferenceAware);
			assertNotNull(((ServiceReferenceAware) service).getServiceReference());

			assertFalse(iter.hasNext());
			time = 111;
			date = new Date(time);
			registrations.add(publishService(date));
			service = iter.next();
			assertTrue(service instanceof Date);
			assertEquals(time, ((Date) service).getTime());
			assertTrue(service instanceof ServiceReferenceAware);
			assertNotNull(((ServiceReferenceAware) service).getServiceReference());

		}
		finally {
			for (int i = 0; i < registrations.size(); i++) {
				((ServiceRegistration) registrations.get(i)).unregister();
			}
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
