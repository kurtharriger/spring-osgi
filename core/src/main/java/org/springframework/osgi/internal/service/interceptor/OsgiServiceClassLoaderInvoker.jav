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
package org.springframework.osgi.internal.service.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.importer.ReferenceClassLoadingOptions;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Around interceptor for thread context classloader (TCCL) handling.
 * 
 * @author Costin Leau
 * 
 */

// FIXME: merge this with OsgiServiceTCCLInvoker and move classloading detection
// logic into a separate class
public class OsgiServiceClassLoaderInvoker implements MethodInterceptor {

	private static final Log log = LogFactory.getLog(OsgiServiceClassLoaderInvoker.class);

	private ClassLoader tccl = null;

	private boolean canCacheClassLoader = false;

	private final ClassLoader clientClassLoader;

	private ClassLoader serviceClassLoader;

	protected int contextClassLoader = ReferenceClassLoadingOptions.UNMANAGED.shortValue();

	private ServiceReference serviceReference;

	public OsgiServiceClassLoaderInvoker(int contextClassLoader, ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader required");

		this.contextClassLoader = contextClassLoader;
		this.clientClassLoader = classLoader;

		// if the reference is not needed create the classloader once and just
		// reuse it
		canCacheClassLoader = !(contextClassLoader == ReferenceClassLoadingOptions.SERVICE_PROVIDER.shortValue());
		if (canCacheClassLoader) {
			this.tccl = determineClassLoader(l, contextClassLoader);
		}

	}

	protected ClassLoader determineClassLoader(ServiceReference reference, int contextClassLoader) {
		boolean trace = log.isTraceEnabled();

		if (ReferenceClassLoadingOptions.CLIENT.shortValue() == contextClassLoader) {
			if (trace) {
				log.trace("client TCCL used for this invocation");
			}
			return clientClassLoader;
		}
		else if (ReferenceClassLoadingOptions.SERVICE_PROVIDER.shortValue() == contextClassLoader) {
			if (trace) {
				log.trace("service provider TCCL used for this invocation");
			}
			if (serviceClassLoader == null) {
				serviceClassLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(reference.getBundle());
			}
			return serviceClassLoader;
		}
		else if (ReferenceClassLoadingOptions.UNMANAGED.shortValue() == contextClassLoader) {
			if (trace) {
				log.trace("no (unmanaged)TCCL used for this invocation");
			}
		}
		return null;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (!canCacheClassLoader)
			tccl = determineClassLoader(context, serviceReference, contextClassLoader);

		ClassLoader oldCL = null;
		boolean trace = log.isTraceEnabled();

		if ((tccl != null && canCacheClassLoader) || !canCacheClassLoader) {
			if (trace)
				log.trace("temporary setting thread context classloader to " + tccl);
			try {
				oldCL = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(tccl);

				return invocation.proceed();
			}
			finally {
				if (trace)
					log.trace("reverting original thread context classloader: " + tccl);
				Thread.currentThread().setContextClassLoader(oldCL);
			}
		}
		// if it's unmanaged
		else {
			return invocation.proceed();
		}
	}

	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof OsgiServiceClassLoaderInvoker) {
			OsgiServiceClassLoaderInvoker oth = (OsgiServiceClassLoaderInvoker) other;
			return contextClassLoader == oth.contextClassLoader && context.equals(oth.context)
					&& clientClassLoader.equals(oth.clientClassLoader)
					&& ObjectUtils.nullSafeEquals(serviceReference, oth.serviceReference);
		}
		return false;
	}

}
