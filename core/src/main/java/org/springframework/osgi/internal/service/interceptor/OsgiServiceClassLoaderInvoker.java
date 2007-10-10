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

import org.aopalliance.intercept.MethodInvocation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.service.importer.ReferenceClassLoadingOptions;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.util.Assert;

/**
 * Add the context classloader handling.
 * 
 * @author Costin Leau
 * 
 */
public abstract class OsgiServiceClassLoaderInvoker extends OsgiServiceInvoker {

	private ClassLoader tccl = null;

	private boolean canCacheClassLoader = false;

	protected final BundleContext context;

	protected ClassLoader clientClassLoader;

	protected ClassLoader serviceClassLoader;

	// TODO: specify a default
	protected int contextClassLoader;

	private ServiceReference serviceReference;

	public OsgiServiceClassLoaderInvoker(ServiceReference reference, int contextClassLoader, ClassLoader classLoader) {
		this(OsgiBundleUtils.getBundleContext(reference.getBundle()), reference, contextClassLoader, classLoader);
	}

	public OsgiServiceClassLoaderInvoker(BundleContext context, ServiceReference reference, int contextClassLoader,
			ClassLoader classLoader) {
		Assert.notNull(context);
		Assert.notNull(classLoader, "ClassLoader required");

		this.context = context;
		this.serviceReference = reference;
		this.contextClassLoader = contextClassLoader;
		this.clientClassLoader = classLoader;

		// if the reference is not needed create the classloader once and just
		// reuse it
		canCacheClassLoader = !(contextClassLoader == ReferenceClassLoadingOptions.SERVICE_PROVIDER);
		if (canCacheClassLoader) {
			this.tccl = determineClassLoader(context, null, contextClassLoader);
		}

	}

	protected ClassLoader determineClassLoader(BundleContext context, ServiceReference reference, int contextClassLoader) {
		boolean trace = log.isTraceEnabled();

		switch (contextClassLoader) {
		case ReferenceClassLoadingOptions.CLIENT: {
			if (trace) {
				log.trace("client TCCL used for this invocation");
			}
			return clientClassLoader;
		}
		case ReferenceClassLoadingOptions.SERVICE_PROVIDER: {
			if (trace) {
				log.trace("service provider TCCL used for this invocation");
			}
			if (serviceClassLoader == null) {
				serviceClassLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(reference.getBundle());
			}
			return serviceClassLoader;
		}
		case ReferenceClassLoadingOptions.UNMANAGED: {
			if (trace) {
				log.trace("no (unmanaged)TCCL used for this invocation");
			}

			break;
		}
		default:
			throw new IllegalStateException("Illegal class loader invocation setting");
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.interceptor.OsgiServiceInvoker#doInvoke(org.osgi.framework.ServiceReference,
	 * org.aopalliance.intercept.MethodInvocation)
	 */
	protected Object doInvoke(Object service, MethodInvocation invocation) throws Throwable {
		if (!canCacheClassLoader)
			tccl = determineClassLoader(context, serviceReference, contextClassLoader);

		ClassLoader oldCL = null;
		boolean trace = log.isTraceEnabled();

		// if it's unmanaged
		if ((tccl != null && canCacheClassLoader) || !canCacheClassLoader) {
			if (trace)
				log.trace("temporary setting thread context classloader to " + tccl);
			try {
				oldCL = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(tccl);

				return super.doInvoke(service, invocation);
			}
			finally {
				if (trace)
					log.trace("reverting original thread context classloader: " + tccl);
				Thread.currentThread().setContextClassLoader(oldCL);
			}
		}
		else {
			return super.doInvoke(service, invocation);
		}
	}
}
