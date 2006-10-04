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

import java.lang.reflect.Method;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.ThrowsAdvice;

/**
 * ThreadLocal management of the BundleContext. This class also functions as advice for
 * temporarily pushing the thread-local context.
 *
 * @author Andy Piper
 */
public class LocalBundleContext implements MethodBeforeAdvice, AfterReturningAdvice, ThrowsAdvice
{
  private final static InheritableThreadLocal contextLocal = new InheritableThreadLocal();

  private BundleContext context;
  private BundleContext savedContext;

  /**
   * Set the local BundleContext to context.
   *
   * @param context
   */
  public static void setContext(BundleContext context) {
    contextLocal.set(context);
  }

  /**
   * Get the local bundle's BundleContext.
   */
  public static BundleContext getContext() {
    return (BundleContext) contextLocal.get();
  }

  public LocalBundleContext(BundleContext bundle) {
    context = bundle;
  }

  public LocalBundleContext(Bundle bundle) {
    this(OsgiResourceUtils.getBundleContext(bundle));
  }

  public synchronized void before(Method method, Object[] args, Object target) throws Throwable {
    this.savedContext = getContext();
    setContext(context);
  }

  public void afterReturning(Object o, Method method, Object[] objects, Object o1) throws Throwable {
    setContext(savedContext);
  }

  public void afterThrowing(Throwable subclass) {
    setContext(savedContext);
  }
}
