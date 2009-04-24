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

package org.springframework.osgi.iandt.proxycreator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.osgi.framework.ServiceReference;
import org.springframework.osgi.iandt.BaseIntegrationTest;

/**
 * Integration test checking the JDK proxy creation on invocation handlers that
 * cannot be seen by the proxy creator.
 * 
 * @author Costin Leau
 * 
 */
public class JdkProxyTest extends BaseIntegrationTest {

	protected String[] getTestBundlesNames() {
		return new String[] { "org.springframework.osgi.iandt,jdk.proxy," + getSpringDMVersion() };
	}

	public void testJDKProxyCreationUsingTheInterfaceClassLoaderInsteadOfTheHandlerOne() throws Exception {
		// get the invocation handler directly
		InvocationHandler handler = getInvocationHandler();
		SomeInterfaceImplementation target = new SomeInterfaceImplementation();
		SomeInterface proxy = createJDKProxy(handler, target);

		SomeInterfaceImplementation.INVOCATION = 0;
		// invoke method on the proxy
		String str = proxy.doSmth();
		// print out the proxy message
		System.out.println("Proxy returned " + str);
		// assert the target wasn't touched
		assertEquals(0, SomeInterfaceImplementation.INVOCATION);
		// check proxy again
		assertSame(handler, Proxy.getInvocationHandler(proxy));

	}

	/**
	 * Poor man's solution.
	 * 
	 * @return
	 */
	private InvocationHandler getInvocationHandler() {
		ServiceReference ref = bundleContext.getServiceReference(InvocationHandler.class.getName());
		if (ref == null)
			throw new IllegalStateException("no invocation handler found");
		return (InvocationHandler) bundleContext.getService(ref);
	}

	private SomeInterface createJDKProxy(InvocationHandler handler, SomeInterface target) {
		return (SomeInterface) Proxy.newProxyInstance(target.getClass().getClassLoader(),
			new Class<?>[] { SomeInterface.class }, handler);
	}
}
