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
package org.springframework.osgi.service.importer.support;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.osgi.service.importer.ImportedOsgiServiceProxy;
import org.springframework.osgi.service.importer.internal.aop.OsgiServiceDynamicInterceptor;
import org.springframework.osgi.service.importer.internal.aop.OsgiServiceTCCLInterceptor;
import org.springframework.osgi.service.importer.internal.aop.ServiceProxyCreator;
import org.springframework.osgi.service.importer.internal.support.RetryTemplate;
import org.springframework.util.Assert;
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
public class OsgiServiceProxyFactoryBean extends AbstractOsgiServiceImportFactoryBean {

	private static final Log log = LogFactory.getLog(OsgiServiceProxyFactoryBean.class);

	private RetryTemplate retryTemplate = new RetryTemplate();

	/** proxy casted to a specific interface to allow specific method calls */
	private ImportedOsgiServiceProxy proxy;

	/** proxy infrastructure hook exposed to allow clean up*/
	private DisposableBean disposable;

	public Class getObjectType() {
		return (proxy != null ? proxy.getClass() : (ObjectUtils.isEmpty(getInterfaces()) ? Object.class
				: getInterfaces()[0]));
	}

	public boolean isSatisfied() {
		if (!isMandatory())
			return true;
		else
			return (proxy == null ? true : proxy.getServiceReference().getBundle() != null);
	}

	protected Object createProxy() {
		if (log.isDebugEnabled())
			log.debug("creating a single service proxy ...");

		final OsgiServiceDynamicInterceptor lookupAdvice = new OsgiServiceDynamicInterceptor(getBundleContext(),
				getUnifiedFilter(), getBeanClassLoader());

		lookupAdvice.setRequiredAtStartup(isMandatory());
		lookupAdvice.setListeners(getListeners());
		lookupAdvice.setRetryTemplate(new RetryTemplate(retryTemplate));

		// add the listeners as a list since it might be updated after the proxy
		// has been created
		lookupAdvice.setDependencyListeners(this.depedencyListeners);
		lookupAdvice.setServiceImporter(this);

		// init target-source
		lookupAdvice.afterPropertiesSet();

		// create a proxy creator using the existing context
		ServiceProxyCreator creator = new AbstractServiceProxyCreator(getInterfaces(), getBeanClassLoader(),
				getBundleContext(), getContextClassLoader()) {

			Advice createDispatcherInterceptor(ServiceReference reference) {
				return lookupAdvice;
			}

			Advice createServiceProviderTCCLAdvice(ServiceReference reference) {
				// FIXME: OSGI-276
				return new OsgiServiceTCCLInterceptor(null);
			}
		};

		disposable = lookupAdvice;

		proxy = (ImportedOsgiServiceProxy) creator.createServiceProxy(lookupAdvice.getServiceReference());
		return proxy;
	}

	DisposableBean getDisposable() {
		return disposable;
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

	public int getRetryTimes() {
		return this.retryTemplate.getRetryNumbers();
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

	public long getTimeout() {
		return this.retryTemplate.getWaitTime();
	}

	/* override to check proper cardinality - x..1 */
	public void setCardinality(Cardinality cardinality) {
		Assert.notNull(cardinality);
		Assert.isTrue(cardinality.isSingle(), "only singular cardinality ('X..1') accepted");
		super.setCardinality(cardinality);
	}
}
