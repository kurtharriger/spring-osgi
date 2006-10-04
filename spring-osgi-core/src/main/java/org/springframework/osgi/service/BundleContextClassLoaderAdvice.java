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
 * Created on 23-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

import java.lang.reflect.Method;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.osgi.framework.Bundle;

/**
 * Around advice that pushes a {@link Bundle}'s {@link ClassLoader} as the context classloader.
 *
 * @author Andy Piper
 */
public class BundleContextClassLoaderAdvice implements MethodBeforeAdvice, AfterReturningAdvice, ThrowsAdvice
{
	private ClassLoader savedContextClassLoader;
	private ClassLoader bundleContextClassLoader;

	public BundleContextClassLoaderAdvice(Bundle bundle) {
		bundleContextClassLoader = new BundleDelegatingClassLoader(bundle);
	}

	public synchronized void before(Method method, Object[] args, Object target) throws Throwable {
		this.savedContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(bundleContextClassLoader);
	}

	public void afterReturning(Object o, Method method, Object[] objects, Object o1) throws Throwable {
		Thread.currentThread().setContextClassLoader(savedContextClassLoader);
	}

	public void afterThrowing(Throwable subclass) {
		Thread.currentThread().setContextClassLoader(savedContextClassLoader);
	}
}
