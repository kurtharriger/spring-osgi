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

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.TestCase;

import org.aopalliance.intercept.MethodInvocation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.mock.MockBundle;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.service.ReferenceClassLoadingOptions;

/**
 * @author Costin Leau
 * 
 */
public class OsgiServiceClassLoaderInvokerTest extends TestCase {

	/**
	 * @author Costin Leau
	 */
	private class TCCLGetter {
		public ClassLoader getTCCL() {
			return Thread.currentThread().getContextClassLoader();
		}
	}

	/**
	 * @author Costin Leau
	 */
	private class TCCLInvoker extends OsgiServiceClassLoaderInvoker {

		public TCCLInvoker(BundleContext context, ServiceReference reference, int contextClassLoader) {
			super(context, reference, contextClassLoader);
		}

		protected Object getTarget() throws Throwable {
			return target;
		}
	}

	private OsgiServiceClassLoaderInvoker invoker;

	private TCCLGetter target;

	private BundleContext context;

	private ServiceReference reference;

	private int classloader;

	// make field static to make sure it is being initialized before the inner
	// class
	// definition inside testDoInvoke is being read

	private static ClassLoader cl;

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		target = new TCCLGetter();
		context = new MockBundleContext();
		reference = new MockServiceReference();
		classloader = ReferenceClassLoadingOptions.CLIENT;

		invoker = new TCCLInvoker(context, reference, classloader);
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		invoker = null;
		reference = null;
		context = null;
		target = null;
	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.service.support.cardinality.OsgiServiceClassLoaderInvoker#doInvoke(java.lang.Object, org.aopalliance.intercept.MethodInvocation)}.
	 */
	public void testDoInvokeWithClientTCCL() throws Throwable {

		Method md = TCCLGetter.class.getMethod("getTCCL", null);
		MethodInvocation invocation = new ReflectiveMethodInvocation(null, null, md, null, null, null);

		OsgiServiceClassLoaderInvokerTest.cl = new URLClassLoader(new URL[0]);

		invoker = new TCCLInvoker(context, reference, ReferenceClassLoadingOptions.CLIENT) {

			protected ClassLoader determineClassLoader(BundleContext context, ServiceReference reference,
					int contextClassLoader) {
				return OsgiServiceClassLoaderInvokerTest.cl;
			}
		};

		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		ClassLoader invocationTCCL = (ClassLoader) invoker.doInvoke(target, invocation);

		assertNotSame("the TCCL hasn't been changed", tccl, invocationTCCL);
		assertSame("the TCCL hasn't been changed", OsgiServiceClassLoaderInvokerTest.cl, invocationTCCL);

		OsgiServiceClassLoaderInvokerTest.cl = null;
	}

	public void testDoInvokeWithServiceTCCL() throws Throwable {
		
		Method md = TCCLGetter.class.getMethod("getTCCL", null);
		MethodInvocation invocation = new ReflectiveMethodInvocation(null, null, md, null, null, null);

		OsgiServiceClassLoaderInvokerTest.cl = new URLClassLoader(new URL[0]);

		invoker = new TCCLInvoker(context, reference, ReferenceClassLoadingOptions.SERVICE_PROVIDER) {

			protected ClassLoader determineClassLoader(BundleContext context, ServiceReference reference,
					int contextClassLoader) {
				return OsgiServiceClassLoaderInvokerTest.cl;
			}
		};

		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		ClassLoader invocationTCCL = (ClassLoader) invoker.doInvoke(target, invocation);

		assertNotSame("the TCCL hasn't been changed", tccl, invocationTCCL);
		assertSame("the TCCL hasn't been changed", OsgiServiceClassLoaderInvokerTest.cl, invocationTCCL);

		OsgiServiceClassLoaderInvokerTest.cl = null;
		invocationTCCL = (ClassLoader) invoker.doInvoke(target, invocation);
		assertSame("the TCCL hasn't been changed", OsgiServiceClassLoaderInvokerTest.cl, invocationTCCL);

	}
	
	public void testDoInvokeWithUnmanagedTCCL() throws Throwable {
		
		Method md = TCCLGetter.class.getMethod("getTCCL", null);
		MethodInvocation invocation = new ReflectiveMethodInvocation(null, null, md, null, null, null);

		OsgiServiceClassLoaderInvokerTest.cl = new URLClassLoader(new URL[0]);

		invoker = new TCCLInvoker(context, reference, ReferenceClassLoadingOptions.UNMANAGED);

		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		ClassLoader invocationTCCL = (ClassLoader) invoker.doInvoke(target, invocation);

		assertSame("the TCCL has been changed", tccl, invocationTCCL);
		assertNotSame("the TCCL has been changed", OsgiServiceClassLoaderInvokerTest.cl, invocationTCCL);

		OsgiServiceClassLoaderInvokerTest.cl = null;
		invocationTCCL = (ClassLoader) invoker.doInvoke(target, invocation);
		assertNotSame("the TCCL has been changed", OsgiServiceClassLoaderInvokerTest.cl, invocationTCCL);

	}

	/**
	 * Test method for
	 * {@link org.springframework.osgi.service.support.cardinality.OsgiServiceClassLoaderInvoker#determineClassLoader(org.osgi.framework.BundleContext, org.osgi.framework.ServiceReference, int)}.
	 */
	public void testDetermineClassLoader() {
		ClassLoader loader = null;
		final Bundle clientBundle = new MockBundle();

		BundleContext ctx = new MockBundleContext() {
			public Bundle getBundle() {
				return clientBundle;
			}
		};

		final Bundle serviceBundle = new MockBundle();
		ServiceReference ref = new MockServiceReference() {

			public Bundle getBundle() {
				return serviceBundle;
			}
		};

		loader = invoker.determineClassLoader(ctx, ref, ReferenceClassLoadingOptions.CLIENT);
		assertEquals(BundleDelegatingClassLoader.createBundleClassLoaderFor(clientBundle), loader);

		loader = invoker.determineClassLoader(ctx, ref, ReferenceClassLoadingOptions.SERVICE_PROVIDER);
		assertEquals(BundleDelegatingClassLoader.createBundleClassLoaderFor(serviceBundle), loader);

		loader = invoker.determineClassLoader(ctx, ref, ReferenceClassLoadingOptions.UNMANAGED);
		assertNull(loader);
	}
}
