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
package org.springframework.osgi.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.osgi.service.TargetSourceLifecycleListener;

/**
 * 
 * @author Costin Leau
 * 
 */
public class TargetSourceLifecycleListenerWrapperTest extends TestCase {

	protected static class JustListener implements TargetSourceLifecycleListener {

		public static int BIND_CALLS = 0;

		public static int UNBIND_CALLS = 0;

		public void bind(Object service, Dictionary properties) throws Exception {
			BIND_CALLS++;
		}

		public void unbind(Object service, Dictionary properties) throws Exception {
			UNBIND_CALLS++;
		}
	}

	protected static class CustomListener {
		public static int BIND_CALLS = 0;

		public static int UNBIND_CALLS = 0;

		public static List BIND_SERVICES = new ArrayList();

		public static List UNBIND_SERVICES = new ArrayList();

		public void myBind(Object service, Dictionary properties) {
			BIND_CALLS++;
			BIND_SERVICES.add(service);

		}

		public void myUnbind(Object service, Dictionary properties) {
			UNBIND_CALLS++;
			UNBIND_SERVICES.add(service);

		}

		public void wrongBind() {
			BIND_CALLS++;
		}

		public void wrongUnbind() {
			UNBIND_CALLS--;
		}
	}

	protected static class CustomAndListener extends JustListener {

		public Integer aBind(Object service, Dictionary props) throws Exception {
			super.bind(service, props);
			return null;
		}

		public void aUnbind(Object service, Dictionary props) throws Exception {
			super.unbind(service, props);
		}
	}

	protected static class OverloadedCustomMethods extends CustomListener {

		public void myBind(Date service, Dictionary properties) {
			super.myBind(service, properties);
		}

		public void myUnbind(String service, Dictionary properties) {
			super.myUnbind(service, properties);
		}
	}

	protected static class ExceptionListener extends CustomAndListener {

		public void bind(Object service, Dictionary properties) throws Exception {
			throw new Exception("expected!") {
				public synchronized Throwable fillInStackTrace() {
					return null;
				}
			};
		}

		public void unbind(Object service, Dictionary properties) throws Exception {
			throw new Exception("expected!") {
				public synchronized Throwable fillInStackTrace() {
					return null;
				}
			};
		}
	}

	protected static class ExceptionCustomListener extends CustomListener {
		public void myBind(Date service, Dictionary properties) {
			throw (RuntimeException) new RuntimeException("expected!") {
				public synchronized Throwable fillInStackTrace() {
					return null;
				}
			};
		}

		public void myUnbind(String service, Dictionary properties) throws IOException {
			throw new IOException("expected!") {
				public synchronized Throwable fillInStackTrace() {
					return null;
				}
			};
		}
	}

	private TargetSourceLifecycleListenerWrapper listener;

	protected void setUp() throws Exception {
		JustListener.BIND_CALLS = 0;
		JustListener.UNBIND_CALLS = 0;
		OverloadedCustomMethods.BIND_SERVICES = new ArrayList();
		OverloadedCustomMethods.UNBIND_SERVICES = new ArrayList();

		CustomListener.BIND_CALLS = 0;
		CustomListener.UNBIND_CALLS = 0;
	}

	protected void tearDown() throws Exception {
		listener = null;
		OverloadedCustomMethods.BIND_SERVICES = null;
		OverloadedCustomMethods.UNBIND_SERVICES = null;
	}

	public void testWrapperOverListener() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new JustListener());
		listener.afterPropertiesSet();

		assertEquals(0, JustListener.BIND_CALLS);
		listener.bind(null, null);
		assertEquals(1, JustListener.BIND_CALLS);
		assertEquals(0, JustListener.UNBIND_CALLS);
		listener.bind(null, null);
		assertEquals(2, JustListener.BIND_CALLS);
		assertEquals(0, JustListener.UNBIND_CALLS);

		listener.unbind(null, null);
		assertEquals(1, JustListener.UNBIND_CALLS);
		assertEquals(2, JustListener.BIND_CALLS);
		listener.unbind(null, null);
		assertEquals(2, JustListener.UNBIND_CALLS);
		assertEquals(2, JustListener.BIND_CALLS);
	}

	public void testWrapperOverNoInvalidClass() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new Object());
		try {
			listener.afterPropertiesSet();
			fail("should have thrown exception");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testWrapperWithIncorrectCustomMethodNames() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new Object());
		listener.setBindMethod("pop");
		listener.setUnbindMethod("corn");

		try {
			listener.afterPropertiesSet();
			fail("should have thrown exception");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testWrapperWithCorrectCustomMethodNamesButIncorrectArgumentTypes() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new CustomListener());
		listener.setBindMethod("wrongBind");
		listener.setUnbindMethod("wrongUnbind");

		assertEquals(0, CustomListener.BIND_CALLS);
		assertEquals(0, CustomListener.UNBIND_CALLS);
		try {
			listener.afterPropertiesSet();
			fail("should have thrown exception");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}

		assertEquals(0, CustomListener.BIND_CALLS);
		assertEquals(0, CustomListener.UNBIND_CALLS);

	}

	public void testWrapperWithCustomMethods() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new CustomListener());
		listener.setBindMethod("myBind");
		listener.setUnbindMethod("myUnbind");
		listener.afterPropertiesSet();

		Object service = new Object();
		assertEquals(0, CustomListener.BIND_CALLS);
		assertEquals(0, CustomListener.UNBIND_CALLS);
		listener.bind(new Object(), null);
		assertEquals(1, CustomListener.BIND_CALLS);
		assertEquals(0, CustomListener.UNBIND_CALLS);

		listener.bind(service, null);
		assertEquals(2, CustomListener.BIND_CALLS);
		assertEquals(0, CustomListener.UNBIND_CALLS);

		listener.unbind(service, null);
		assertEquals(2, CustomListener.BIND_CALLS);
		assertEquals(1, CustomListener.UNBIND_CALLS);

		listener.unbind(service, null);
		assertEquals(2, CustomListener.BIND_CALLS);
		assertEquals(2, CustomListener.UNBIND_CALLS);
	}

	public void testWrapperWithCustomMethodsAndNullParameters() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new CustomListener());
		listener.setBindMethod("myBind");
		listener.setUnbindMethod("myUnbind");
		listener.afterPropertiesSet();

		assertEquals(0, CustomListener.BIND_CALLS);
		assertEquals(0, CustomListener.UNBIND_CALLS);
		listener.bind(null, null);
		assertEquals("null services allowed", 1, CustomListener.BIND_CALLS);

		listener.unbind(null, null);

		assertEquals("null services allowed", 1, CustomListener.UNBIND_CALLS);
	}

	public void testWrapperWithBothCustomAndInterfaceMethods() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new CustomAndListener());
		listener.setBindMethod("aBind");
		listener.setUnbindMethod("aUnbind");
		listener.afterPropertiesSet();

		Object service = new Object();

		assertEquals(0, CustomAndListener.BIND_CALLS);
		assertEquals(0, CustomAndListener.UNBIND_CALLS);
		listener.bind(service, null);
		assertEquals(2, CustomAndListener.BIND_CALLS);
		assertEquals(0, CustomAndListener.UNBIND_CALLS);

		listener.unbind(service, null);
		assertEquals(2, CustomAndListener.BIND_CALLS);
		assertEquals(2, CustomAndListener.UNBIND_CALLS);

	}

	public void testWrapperWithCustomOverloadedMethodsAndDifferentServiceTypes() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new OverloadedCustomMethods());
		listener.setBindMethod("myBind");
		listener.setUnbindMethod("myUnbind");
		listener.afterPropertiesSet();

		Object objService = new Object();
		Date dateService = new Date();
		String stringService = "token";

		assertEquals(0, OverloadedCustomMethods.BIND_CALLS);
		assertEquals(0, OverloadedCustomMethods.UNBIND_CALLS);
		listener.bind(objService, null);
		assertEquals("only one method accepts Object(s)", 1, OverloadedCustomMethods.BIND_CALLS);
		assertEquals(0, OverloadedCustomMethods.UNBIND_CALLS);

		listener.bind(dateService, null);
		assertEquals("two method accept Date(s)", 3, OverloadedCustomMethods.BIND_CALLS);
		assertEquals(0, OverloadedCustomMethods.UNBIND_CALLS);

		listener.unbind(stringService, null);
		assertEquals("two method accept Date(s)", 3, OverloadedCustomMethods.BIND_CALLS);
		assertEquals("two method accept String(s)", 2, OverloadedCustomMethods.UNBIND_CALLS);

		listener.unbind(objService, null);
		assertEquals("two method accept Date(s)", 3, OverloadedCustomMethods.BIND_CALLS);
		assertEquals("only one method accepts Object(s)", 3, OverloadedCustomMethods.UNBIND_CALLS);

		assertEquals(3, OverloadedCustomMethods.BIND_SERVICES.size());
		assertSame("incorrect call order", objService, OverloadedCustomMethods.BIND_SERVICES.get(0));
		assertSame("incorrect call order", dateService, OverloadedCustomMethods.BIND_SERVICES.get(1));
		assertSame("incorrect call order", dateService, OverloadedCustomMethods.BIND_SERVICES.get(2));

		assertEquals(3, OverloadedCustomMethods.UNBIND_SERVICES.size());
		assertSame("incorrect call order", stringService, OverloadedCustomMethods.UNBIND_SERVICES.get(0));
		assertSame("incorrect call order", stringService, OverloadedCustomMethods.UNBIND_SERVICES.get(1));
		assertSame("incorrect call order", objService, OverloadedCustomMethods.UNBIND_SERVICES.get(2));
	}

	public void testExceptionOnListenerMethod() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new ExceptionListener());
		listener.setBindMethod("aBind");
		listener.setUnbindMethod("aUnbind");
		listener.afterPropertiesSet();

		Object service = new Object();
		assertEquals(0, JustListener.BIND_CALLS);
		assertEquals(0, JustListener.UNBIND_CALLS);
		listener.bind(service, null);
		assertEquals(1, JustListener.BIND_CALLS);
		assertEquals(0, JustListener.UNBIND_CALLS);

		listener.unbind(service, null);
		assertEquals(1, JustListener.BIND_CALLS);
		assertEquals(1, JustListener.UNBIND_CALLS);
	}

	public void testExceptionOnCustomMethods() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new ExceptionCustomListener());
		listener.setBindMethod("myBind");
		listener.setUnbindMethod("myUnbind");
		listener.afterPropertiesSet();

		Date service = new Date();
		assertEquals(0, ExceptionCustomListener.BIND_CALLS);
		assertEquals(0, ExceptionCustomListener.UNBIND_CALLS);
		listener.bind(service, null);
		assertEquals(1, ExceptionCustomListener.BIND_CALLS);
		assertEquals(0, ExceptionCustomListener.UNBIND_CALLS);

		listener.unbind(service, null);
		assertEquals(1, ExceptionCustomListener.BIND_CALLS);
		assertEquals(1, ExceptionCustomListener.UNBIND_CALLS);
	}

	public void testStandardListenerWithListeningMethodsSpecifiedAsCustomOnes() throws Exception {
		listener = new TargetSourceLifecycleListenerWrapper(new JustListener());
		listener.setBindMethod("bind");
		listener.setUnbindMethod("unbind");
		listener.afterPropertiesSet();

		Object service = null;
		assertEquals(0, JustListener.BIND_CALLS);
		listener.bind(service, null);
		// only the interface is being called since the service is null
		assertEquals(2, JustListener.BIND_CALLS);

		service = new Object();

		listener.bind(service, null);
		assertEquals(4, JustListener.BIND_CALLS);
	}
}
