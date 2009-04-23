/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.compendium.internal.cm;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.osgi.compendium.internal.cm.ManagedFactoryDisposableInvoker.DestructionCodes;

/**
 * @author Costin Leau
 */
public class ManagedFactoryDisposableInvokerTest extends TestCase {

	enum Action {
		INTERFACE, SPRING_CUSTOM_METHOD, OSGI_CUSTOM_METHOD;
	}


	private List<Action> actions;


	class A implements DisposableBean {

		public void destroy() throws Exception {
			actions.add(Action.INTERFACE);
		}

	}

	class B extends A {

		public void stop() {
			actions.add(Action.SPRING_CUSTOM_METHOD);
		}
	}

	class C extends B {

		public void stop(int code) {
			actions.add(Action.OSGI_CUSTOM_METHOD);
		}
	}

	class D extends A {

		public void stop(int code) {
			actions.add(Action.OSGI_CUSTOM_METHOD);
		}
	}

	class E {

		public void stop() {
			actions.add(Action.SPRING_CUSTOM_METHOD);
		}

		public void stop(int code) {
			actions.add(Action.OSGI_CUSTOM_METHOD);
		}
	}


	private ManagedFactoryDisposableInvoker invoker;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		actions = new ArrayList<Action>();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		invoker = null;
		actions = null;
	}

	public void testDefinitionWithCustomMethods() throws Exception {
		C c = new C();
		invoker = new ManagedFactoryDisposableInvoker(C.class, "stop");
		assertTrue(actions.isEmpty());
		doInvoke(c);
		assertEquals(3, actions.size());
		assertSame(Action.INTERFACE, actions.get(0));
		assertSame(Action.SPRING_CUSTOM_METHOD, actions.get(1));
		assertSame(Action.OSGI_CUSTOM_METHOD, actions.get(2));
	}

	public void testInterfaceAndSpringMethod() throws Exception {
		B b = new B();
		invoker = new ManagedFactoryDisposableInvoker(B.class, "stop");
		assertTrue(actions.isEmpty());
		doInvoke(b);
		assertEquals(2, actions.size());
		assertSame(Action.INTERFACE, actions.get(0));
		assertSame(Action.SPRING_CUSTOM_METHOD, actions.get(1));
	}

	public void testInterfaceAndOsgiMethod() throws Exception {
		D d = new D();
		invoker = new ManagedFactoryDisposableInvoker(D.class, "stop");
		assertTrue(actions.isEmpty());
		doInvoke(d);
		assertEquals(2, actions.size());
		assertSame(Action.INTERFACE, actions.get(0));
		assertSame(Action.OSGI_CUSTOM_METHOD, actions.get(1));
	}

	public void testSpringAndOsgiMethod() throws Exception {
		E e = new E();
		invoker = new ManagedFactoryDisposableInvoker(E.class, "stop");
		assertTrue(actions.isEmpty());
		doInvoke(e);
		assertEquals(2, actions.size());
		assertSame(Action.SPRING_CUSTOM_METHOD, actions.get(0));
		assertSame(Action.OSGI_CUSTOM_METHOD, actions.get(1));
	}

	public void testNoMethod() throws Exception {
		invoker = new ManagedFactoryDisposableInvoker(Object.class, "stop");
		doInvoke(new Object());
		assertEquals(0, actions.size());
	}

	private void doInvoke(Object target) {
		invoker.destroy("test", target, DestructionCodes.BUNDLE_STOPPING);
	}
}