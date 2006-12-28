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
 * Created on 26-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

import java.lang.reflect.Method;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.target.HotSwappableTargetSource;

/**
 * @author Adrian Colyer
 * @author Hal Hildebrand
 * @since 2.0
 */
public class OsgiServiceInterceptor implements MethodBeforeAdvice {
	private final Object unavailableService;
	private final HotSwappableTargetSource targetSource;
	private int maxRetries = OsgiServiceProxyFactoryBean.DEFAULT_MAX_RETRIES;
	private long retryIntervalMillis = OsgiServiceProxyFactoryBean.DEFAULT_MILLIS_BETWEEN_RETRIES;

	public OsgiServiceInterceptor(HotSwappableTargetSource targetSource, Object unavailableService) {
		this.targetSource = targetSource;
		this.unavailableService = unavailableService;
	}

	/**
	 * The maximum number of times that we should attempt to rebind to a
	 * service that has been unregistered.
	 *
	 * @param maxRetries
	 */
	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	/**
	 * Number of milliseconds to wait between retry attempts when the target
	 * service has been unregistered.
	 *
	 * @param interval
	 */
	public void setRetryIntervalMillis(long interval) {
		this.retryIntervalMillis = interval;
	}

	/* (non-Javadoc)
	 * @see org.springframework.aop.MethodBeforeAdvice#before(java.lang.reflect.Method, java.lang.Object[], java.lang.Object)
	 */
	public void before(Method method, Object[] args, Object target) throws Throwable {
		int numAttempts = 0;
		while (targetSource.getTarget() == unavailableService && (numAttempts++ < this.maxRetries)) {
			Thread.sleep(this.retryIntervalMillis);
		}
	}
}
