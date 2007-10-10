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
package org.springframework.osgi.service.importer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Filter;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.osgi.internal.context.support.LocalBundleContext;
import org.springframework.osgi.internal.service.interceptor.OsgiServiceDynamicInterceptor;
import org.springframework.osgi.internal.service.interceptor.ServiceReferenceAwareAdvice;
import org.springframework.osgi.internal.service.support.RetryTemplate;
import org.springframework.osgi.internal.util.DebugUtils;
import org.springframework.osgi.service.ServiceReferenceAware;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.osgi.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Specialized single-service proxy creator. Will return a proxy that will
 * select only one OSGi service which matches the configuration criteria. If the
 * selected service goes away, the proxy will search for a replacement.
 * 
 * @author Costin Leau
 * @author Adrian Colyer
 * @author Hal Hildebrand
 * 
 */
public class OsgiServiceProxyFactoryBean extends AbstractOsgiServiceProxyFactoryBean {

	private static final Log log = LogFactory.getLog(OsgiServiceProxyFactoryBean.class);

	protected RetryTemplate retryTemplate = new RetryTemplate();

	private ServiceReferenceAware proxy;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.AbstractOsgiServiceProxyFactoryBean#destroy()
	 */
	public void destroy() throws Exception {
		// FIXME: implement cleanup
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.AbstractOsgiServiceProxyFactoryBean#getObject()
	 */
	public Object getObject() {
		if (!initialized)
			throw new FactoryBeanNotInitializedException();

		if (proxy == null) {
			proxy = createSingleServiceProxy(serviceTypes, listeners, classLoader);
		}

		return proxy;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.AbstractOsgiServiceProxyFactoryBean#getObjectType()
	 */
	public Class getObjectType() {
		return (proxy != null ? proxy.getClass() : (ObjectUtils.isEmpty(serviceTypes) ? Object.class : serviceTypes[0]));

	}

	public boolean isSatisfied() {
		if (!isMandatory())
			return true;
		else
			return (proxy == null ? true : proxy.getServiceReference().getBundle() != null);
	}

	protected ServiceReferenceAware createSingleServiceProxy(Class[] classes,
			TargetSourceLifecycleListener[] listeners, ClassLoader loader) {
		if (log.isDebugEnabled())
			log.debug("creating a singleService proxy");

		ProxyFactory factory = new ProxyFactory();

		// mold the proxy
		ClassUtils.configureFactoryForClass(factory, classes);

		// TODO: the same advices should be available for the multi case/service
		// collection

		// Bundle Ctx
		addLocalBundleContextSupport(factory);

		// dynamic retry interceptor / context classloader / service aware
		// implementation
		addOsgiRetryInterceptor(factory, getUnifiedFilter(), listeners);

		// TODO: should these be enabled ?
		// factory.setFrozen(true);
		// factory.setOptimize(true);
		// factory.setOpaque(true);

		try {
			return (ServiceReferenceAware) factory.getProxy(loader);
		}
		catch (NoClassDefFoundError ncdfe) {
			if (log.isWarnEnabled()) {
				DebugUtils.debugNoClassDefFoundWhenProxying(ncdfe, bundleContext, serviceTypes);
			}
			throw ncdfe;
		}
	}

	/**
	 * Add the retry interceptor and the ServiceReference aware mixin.
	 * 
	 * @param factory
	 * @param filter
	 * @param listeners
	 */
	protected void addOsgiRetryInterceptor(ProxyFactory factory, Filter filter,
			TargetSourceLifecycleListener[] listeners) {
		OsgiServiceDynamicInterceptor lookupAdvice = new OsgiServiceDynamicInterceptor(bundleContext,
				contextClassloader, mandatory, classLoader);
		lookupAdvice.setListeners(listeners);
		lookupAdvice.setFilter(filter);
		lookupAdvice.setRetryTemplate(new RetryTemplate(retryTemplate));

		// add the listeners as a list since it might be updated after the proxy has been created
		lookupAdvice.setDependencyListeners(this.depedencyListeners);
		lookupAdvice.setServiceImporter(this);

		lookupAdvice.afterPropertiesSet();

		factory.addAdvice(new ServiceReferenceAwareAdvice(lookupAdvice));
		factory.addAdvice(lookupAdvice);
	}

	/**
	 * Add the local bundle context support.
	 * 
	 * @param factory
	 */
	protected void addLocalBundleContextSupport(ProxyFactory factory) {
		// Add advice for pushing the bundle context
		factory.addAdvice(new LocalBundleContext(bundleContext.getBundle()));
	}

	/**
	 * How many times should we attempt to rebind to a target service if the
	 * service we are currently using is unregistered. Default is 3 times. <p/>
	 * Changing this property after initialization is complete has no effect.
	 * 
	 * @param maxRetries The maxRetries to set.
	 */
	public void setRetryTimes(int maxRetries) {
		this.retryTemplate.setRetryNumbers(maxRetries);
	}

	/**
	 * How long should we wait between failed attempts at rebinding to a service
	 * that has been unregistered. <p/>
	 * 
	 * @param millisBetweenRetries The millisBetweenRetries to set.
	 */
	public void setTimeout(long millisBetweenRetries) {
		this.retryTemplate.setWaitTime(millisBetweenRetries);
	}
}
