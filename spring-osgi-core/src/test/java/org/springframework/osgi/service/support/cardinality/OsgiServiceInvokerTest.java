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
package org.springframework.osgi.service.support.cardinality;

import junit.framework.TestCase;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ReflectiveMethodInvocation;

/**
 * @author Costin Leau
 * 
 */
public class OsgiServiceInvokerTest extends TestCase {

	private OsgiServiceInvoker invoker;

	private Object target;

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		target = new Object();
		invoker = new OsgiServiceInvoker() {
			protected Object getTarget() throws Throwable {
				return target;
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		target = null;
		invoker = null;
	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.service.support.cardinality.OsgiServiceInvoker#invoke(org.aopalliance.intercept.MethodInvocation)}.
	 */
	public void testInvoke() throws Throwable {
		MethodInvocation invocation = new ReflectiveMethodInvocation(new Object(), new Object(), Object.class
				.getMethod("hashCode", null), null, null, null);
		Object result = invoker.invoke(invocation);
		assertEquals("different target invoked", new Integer(target.hashCode()), result);
	}
}
