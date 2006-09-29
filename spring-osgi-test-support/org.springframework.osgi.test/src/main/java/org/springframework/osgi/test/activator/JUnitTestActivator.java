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
package org.springframework.osgi.test.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.test.OsgiJUnitTest;
import org.springframework.osgi.test.runner.TestRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * Test bundle activator - looks for a predefined JUnit test runner and triggers
 * the test execution.
 * 
 * @author Costin Leau
 * 
 */
public class JUnitTestActivator implements BundleActivator {

	private BundleContext context;
	private ServiceReference reference;
	private ServiceRegistration registration;
	private TestRunner service;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		this.context = bc;

		reference = context.getServiceReference(TestRunner.class.getName());
		if (reference == null)
			throw new IllegalArgumentException("cannot find service at " + TestRunner.class.getName());
		service = (TestRunner) context.getService(reference);

		registration = context.registerService(JUnitTestActivator.class.getName(), this, new Hashtable());

		// load class
		// ClassLoader loader = getClass().getClassLoader();
		// System.out.println("loader is " + loader);
		// System.out.println(loader.loadClass("org.foo.bar.test.BasicTests"));
		// System.out.println("found resource "
		// +
		// loader.getResource("org/springframework/osgi/test/BundleCreationTests.class"));

	}

	public void debugClass(String className) {
		try {
			Class clazz = getClass().getClassLoader().loadClass(className);
			Constructor[] constructors = clazz.getConstructors();
			System.out.println("constructors " + Arrays.toString(constructors));
			System.out.println("declared constru " + Arrays.toString(clazz.getDeclaredConstructors()));
			System.out.println("modified is " + Modifier.toString(clazz.getModifiers()));
		}
		catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void executeTest() {
		service.runTest(loadTest());
	}

	private OsgiJUnitTest loadTest() {
		String testClass = System.getProperty(OsgiJUnitTest.OSGI_TEST);
		if (testClass == null)
			throw new IllegalArgumentException("no test class specified under " + OsgiJUnitTest.OSGI_TEST);

		try {
			Class clazz = getClass().getClassLoader().loadClass(testClass);
			OsgiJUnitTest test = (OsgiJUnitTest) clazz.newInstance();
			test.setBundleContext(context);
			return test;
		}
		catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		catch (InstantiationException ex) {
			throw new RuntimeException(ex);
		}
		catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) throws Exception {
		bc.ungetService(reference);
		registration.unregister();
	}

}
