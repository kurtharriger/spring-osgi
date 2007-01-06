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
 *
 */
package org.springframework.osgi.context.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * ThreadLocal management of the BundleContext. This class also functions as
 * advice for temporarily pushing the thread-local context.
 * 
 * @author Andy Piper
 * @author Costin Leau
 */
public class LocalBundleContext implements MethodInterceptor {

	private final static InheritableThreadLocal contextLocal = new InheritableThreadLocal();

	private final BundleContext context;

	/**
	 * Get the local bundle's BundleContext.
	 */
	public static BundleContext getContext() {
		return (BundleContext) contextLocal.get();
	}

	public static void setContext(BundleContext bundle) {
		contextLocal.set(bundle);
	}

	public LocalBundleContext(BundleContext bundle) {
		this.context = bundle;
	}

	public LocalBundleContext(Bundle bundle) {
		this(OsgiResourceUtils.getBundleContext(bundle));
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		// save the old context
		Object oldContext = contextLocal.get();

		try {
			contextLocal.set(context);
			return invocation.proceed();
		}
		finally {
			// restore old context
			contextLocal.set(oldContext);
		}
	}
}
