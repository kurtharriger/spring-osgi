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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestResult;

/**
 * Utility class for running OSGi-JUnit tests.
 * 
 * @author Costin Leau
 * 
 */
public abstract class TestUtils {

	/**
	 * Serialize the test result using the given OutputStream.
	 * 
	 * @param result
	 * @param stream
	 */
	public static void sendTestResult(TestResult result, ObjectOutputStream stream) {

		List errorList = new ArrayList();
		Enumeration errors = result.errors();
		while (errors.hasMoreElements()) {
			TestFailure failure = (TestFailure) errors.nextElement();
			errorList.add(failure.thrownException());
		}
		List failureList = new ArrayList();
		Enumeration failures = result.failures();
		while (failures.hasMoreElements()) {
			TestFailure failure = (TestFailure) failures.nextElement();
			failureList.add(failure.thrownException());
		}

		try {
			stream.writeObject(errorList);
			stream.writeObject(failureList);
			stream.flush();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Deserialize testResult from the given ObjectInputStream. The loaded
	 * failures/errors are added to the given testResult.
	 * 
	 * @param testResult the test result to which the properties are added
	 * @param stream the stream used to load the test result
	 * @param test the test used for adding the TestResult errors/failures
	 */
	public static void receiveTestResult(TestResult testResult, Test test, ObjectInputStream stream) {
		// deserialize back the TestResult
		List errors;
		List failures;
		try {
			errors = (List) stream.readObject();
			failures = (List) stream.readObject();
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}

		// get errors
		for (Iterator iter = errors.iterator(); iter.hasNext();) {
			testResult.addError(test, (Throwable) iter.next());
		} // get failures
		for (Iterator iter = failures.iterator(); iter.hasNext();) {
			testResult.addFailure(test, (AssertionFailedError) iter.next());
		}
	}

	public static void closeStream(InputStream stream) {
		if (stream != null)
			try {
				stream.close();
			}
			catch (IOException ex) {
				// ignore
			}
	}

	public static void closeStream(OutputStream stream) {
		if (stream != null)
			try {
				stream.close();
			}
			catch (IOException ex) {
				// ignore
			}
	}
}
