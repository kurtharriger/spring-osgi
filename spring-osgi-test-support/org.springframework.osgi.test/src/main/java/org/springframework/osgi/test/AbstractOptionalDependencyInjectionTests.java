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
package org.springframework.osgi.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * JUnit superclass, which makes context creation optional (and injection)
 * optional. Required for mixing Spring existing testing hierarchy with the OSGi
 * testing framework functionality.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractOptionalDependencyInjectionTests extends AbstractDependencyInjectionSpringContextTests {

	private class DummyConfigurableListableBeanFactory implements InvocationHandler {
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// don't do anything
			return null;
		}
	}

	protected static final String APP_CONTEXT_LOADING_DISABLED = "org.springframework.osgi.test.appCtx.disabled";

	public AbstractOptionalDependencyInjectionTests() {
		super();
	}

	public AbstractOptionalDependencyInjectionTests(String name) {
		super(name);
	}

	/**
	 * Create an Osgi bundle based XML application context. If there are no
	 * locations given (the default), the autowiring and context creation are
	 * cancelled.
	 */
	protected ConfigurableApplicationContext loadContextLocations(String[] locations) throws Exception {

		if (ObjectUtils.isEmpty(locations)) {
			logger.info("No application context location specified; disabling injection");
			System.getProperties().put(APP_CONTEXT_LOADING_DISABLED, Boolean.TRUE.toString());
			setAutowireMode(AUTOWIRE_NO);
			setPopulateProtectedVariables(false);

			// return a fake object to preserve contract
			return (ConfigurableApplicationContext) Proxy.newProxyInstance(ConfigurableApplicationContext.class
					.getClassLoader(), new Class[] { ConfigurableApplicationContext.class },
					new DummyConfigurableListableBeanFactory());

		}

		else {
			// TODO: the load Count is not changed (since it's not accessible)
			if (logger.isInfoEnabled()) {
				logger.info("Loading context for locations: " + StringUtils.arrayToCommaDelimitedString(locations));
			}

			return createApplicationContext(locations);
		}
	}

	protected void prepareTestInstance() throws Exception {
		// skip injection
		if (System.getProperty(APP_CONTEXT_LOADING_DISABLED)!= null)
			return;
		else
			super.prepareTestInstance();
	}

	protected abstract ConfigurableApplicationContext createApplicationContext(String[] locations);
}
