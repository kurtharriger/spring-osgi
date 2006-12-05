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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Properties;

import junit.framework.Protectable;
import junit.framework.TestCase;
import junit.framework.TestResult;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.test.runner.TestRunner;

/**
 * OSGi service for executing JUnit tests.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiJUnitService implements BundleActivator, TestRunner {

	private ServiceRegistration registration;

	/**
	 * Write the test result.
	 */
	private ObjectOutputStream outStream;

	/**
	 * Read the test name to execute.
	 */
	private ObjectInputStream inStream;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		registration = bc.registerService(TestRunner.class.getName(), this, new Hashtable());
	}

	protected void setupStreams() throws Exception {
		Properties props = System.getProperties();

		byte[] outSource = (byte[]) props.get(OsgiJUnitTest.FROM_OSGI);
		this.outStream = new ObjectOutputStream(new ConfigurableByteArrayOutputStream(outSource));
		// write header
		this.outStream.flush();

		byte[] inSource = (byte[]) props.get(OsgiJUnitTest.FOR_OSGI);
		this.inStream = new ObjectInputStream(new ByteArrayInputStream(inSource));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) throws Exception {
		registration.unregister();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.TestRunner#runTest(junit.framework.Test,
	 *      junit.framework.TestResult)
	 */
	public void runTest(OsgiJUnitTest test) {
		try {
			executeTest(test);
		}
		catch (Exception ex) {
		        if (ex instanceof RuntimeException) {
			    throw (RuntimeException) ex;
		        }
			throw new RuntimeException("cannot execute test:" + ex, ex);
		}
	}

	/**
	 * Execute the JUnit test and publish results to the outside-OSGi world.
	 * 
	 * @param test
	 * @throws Exception
	 */
	protected void executeTest(OsgiJUnitTest test) throws Exception {
		try {
			setupStreams();

			// read the test to be executed
			String testName = inStream.readUTF();
			// execute the test
			TestResult result = runTest(test, testName);

			// write result back to the stream
			TestUtils.sendTestResult(result, outStream);
		}
		finally {
			TestUtils.closeStream(outStream);
			TestUtils.closeStream(inStream);
		}
	}

	/**
	 * Run fixture setup, test from the given test case and fixture teardown.
	 * 
	 * @param testCase
	 * @param testName 
	 */
	protected TestResult runTest(final OsgiJUnitTest testCase, String testName) {
		final TestResult result = new TestResult();
		testCase.setName(testName);

		try {
			testCase.onSetUp();

			try {
				// use TestResult method to bypass the onSetup/tearDown methods
				result.runProtected((TestCase) testCase, new Protectable() {

					public void protect() throws Throwable {
						testCase.osgiRunTest();
					}

				});
			}
			finally {
				testCase.onTearDown();
			}
		}
		// reflection exceptions
		catch (Exception ex) {
		        if (ex instanceof RuntimeException) {
			    throw (RuntimeException) ex;
		        }
			throw new RuntimeException("test execution failed;" + ex, ex);
		}
		return result;
	}
}
