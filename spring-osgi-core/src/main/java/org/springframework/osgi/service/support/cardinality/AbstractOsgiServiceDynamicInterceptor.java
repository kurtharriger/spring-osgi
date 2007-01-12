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

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.service.support.RetryTemplate;

/**
 * Base class to isolated common elements between various OSGi interceptors.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractOsgiServiceDynamicInterceptor implements BundleContextAware, MethodInterceptor {

	protected final Log logger = LogFactory.getLog(getClass());

	protected RetryTemplate retryTemplate = new RetryTemplate();

	protected String clazz;

	protected String filter;

	protected BundleContext context;

	public void setClass(String clazz) {
		this.clazz = clazz;
	}

	public void setBundleContext(BundleContext context) {
		this.context = context;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	public RetryTemplate getRetryTemplate() {
		return retryTemplate;
	}

}
