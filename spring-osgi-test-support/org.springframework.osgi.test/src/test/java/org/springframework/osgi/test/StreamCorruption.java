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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.springframework.util.ReflectionUtils;

/**
 * Test JUnit stream corruption; basically test that the stream size is suitable
 * enought.
 * 
 * @author Costin Leau
 * 
 */
public class StreamCorruption extends TestCase {

	private ObjectInputStream in;
	private ObjectOutputStream out;
	private AbstractOsgiTests osgiTest;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		osgiTest = new AbstractOsgiTests() {
		};

		// call AbstractOsgiTest to setup the streams
		invokeOsgiTestMethod(osgiTest, "setupStreams");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (in != null) {
			try {
				in.close();
			}
			catch (Exception ex) {
				// ignore
			}
		}

		if (out != null) {
			try {
				out.close();
			}
			catch (Exception ex) {
				// ignore
			}
		}
		
		invokeOsgiTestMethod(osgiTest, "cleanupStreams");
	}

	public void testOutputStreamSize() throws Exception {
		Throwable throwable;

		// build a huge stacktrace
		try {
			try {
				try {
					throw new IllegalArgumentException("root cause").fillInStackTrace();
				}
				catch (Throwable ex1) {
					throw new UnsupportedOperationException(ex1).fillInStackTrace();
				}
			}
			catch (Throwable ex2) {
				throw new UnsupportedOperationException(ex2).fillInStackTrace();
			}
		}
		catch (Throwable ex3) {
			Exception ex = new Exception("foo-bar",
					new Exception(new Exception(new Exception(new Exception(new RuntimeException(new RuntimeException(
							new ClassNotFoundException("boo", new RuntimeException(ex3)))))))).fillInStackTrace());

			throwable = ex3;
		}

		byte[] array = (byte[]) System.getProperties().get(OsgiJUnitTest.FROM_OSGI);

		out = new ObjectOutputStream(new ConfigurableByteArrayOutputStream(array));

		// write the exception
		out.writeObject(throwable);
		out.flush();

		in = new ObjectInputStream(new ByteArrayInputStream(array));
		Throwable read = (Throwable) in.readObject();
		assertEquals(throwable.getMessage(), read.getMessage());
		assertEquals(throwable.getClass(), read.getClass());
	}

	private void invokeOsgiTestMethod(Object obj, String name) throws Exception {
		Method method = AbstractOsgiTests.class.getDeclaredMethod(name, null);
		method.setAccessible(true);
		method.invoke(obj, null);
	}

	private Object readField(Object obj, String name) throws Exception {
		Field field = obj.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(obj);
	}

	private void writeField(Object obj, String name, Object value) throws Exception {
		Field field = obj.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(obj, value);
	}
}
