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

import org.aopalliance.intercept.MethodInvocation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.context.support.OsgiResourceUtils;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean.ReferenceClassLoadingOptions;

/**
 * Add the context classloader handling.
 * 
 * @author Costin Leau
 * 
 */
public abstract class OsgiServiceClassLoaderInvoker extends OsgiServiceInvoker {

	private ClassLoader tccl = null;

	private boolean canCacheClassLoader = false;

	protected BundleContext context;

	private int contextClassLoader;

	private ServiceReference serviceReference;

	public OsgiServiceClassLoaderInvoker(ServiceReference reference, int contextClassLoader) {
		this(OsgiResourceUtils.getBundleContext(reference.getBundle()), reference, contextClassLoader);
	}

	public OsgiServiceClassLoaderInvoker(BundleContext context, ServiceReference reference, int contextClassLoader) {
		this.context = context;
		this.serviceReference = reference;

		// if the reference is not needed create the classloader once and just
		// reuse it
		canCacheClassLoader = !(contextClassLoader == ReferenceClassLoadingOptions.SERVICE_PROVIDER);
		if (canCacheClassLoader)
			this.tccl = determineClassLoader(context, null, contextClassLoader);
	}

	protected ClassLoader determineClassLoader(BundleContext context, ServiceReference reference, int contextClassLoader) {
		boolean trace = log.isTraceEnabled();

		switch (contextClassLoader) {
		case ReferenceClassLoadingOptions.CLIENT:
			if (trace)
				log.trace("client TCCL used for this invocation");
			return BundleDelegatingClassLoader.createBundleClassLoaderFor(context.getBundle());
		case ReferenceClassLoadingOptions.SERVICE_PROVIDER:
			if (trace)
				log.trace("service provider TCCL used for this invocation");
			return BundleDelegatingClassLoader.createBundleClassLoaderFor(reference.getBundle());
		case ReferenceClassLoadingOptions.UNMANAGED:
			if (trace)
				log.trace("no (unmanaged)TCCL used for this invocation");

			break;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.support.cardinality.OsgiServiceInvoker#doInvoke(org.osgi.framework.ServiceReference,
	 * org.aopalliance.intercept.MethodInvocation)
	 */
	protected Object doInvoke(Object service, MethodInvocation invocation) throws Throwable {
		if (!canCacheClassLoader)
			tccl = determineClassLoader(context, serviceReference, contextClassLoader);

		ClassLoader oldCL = null;
		if (tccl != null) {
			if (log.isTraceEnabled())
				log.trace("temporary setting thread context classloader to " + tccl);
			try {
				oldCL = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(tccl);

				return super.doInvoke(service, invocation);
			}
			finally {
				if (log.isTraceEnabled())
					log.trace("reverting original thread context classloader: " + tccl);
				Thread.currentThread().setContextClassLoader(oldCL);
			}
		}

		return super.doInvoke(service, invocation);

	}
}
