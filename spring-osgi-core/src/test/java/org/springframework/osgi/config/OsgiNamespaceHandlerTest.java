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
package org.springframework.osgi.config;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.easymock.internal.AlwaysMatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean;

/**
 * UnitTest for OSGi namespace handler
 * 
 * @author Costin Leau
 * 
 */
public class OsgiNamespaceHandlerTest extends TestCase {

	private GenericApplicationContext appContext;
	private MockControl bundleCtxCtrl;
	private BundleContext bundleContext;

	ServiceReference mockReference;

	private class MockReference implements ServiceReference {

		public Bundle getBundle() {
			return null;
		}

		public Object getProperty(String key) {
			return null;
		}

		public String[] getPropertyKeys() {
			return null;
		}

		public Bundle[] getUsingBundles() {
			return null;
		}

		public boolean isAssignableTo(Bundle bundle, String className) {
			return false;
		}

	}

	public void setUp() throws Exception {
		bundleCtxCtrl = MockControl.createNiceControl(BundleContext.class);
		bundleContext = (BundleContext) bundleCtxCtrl.getMock();

		mockReference = new MockReference();
		bundleCtxCtrl.expectAndReturn(bundleContext.getServiceReferences(Serializable.class.getName(), null),
				new ServiceReference[] { mockReference });

		appContext = new GenericApplicationContext();
		appContext.getBeanFactory().addBeanPostProcessor(new BundleContextAwareProcessor(bundleContext));

		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		// reader.setEventListener(this.listener);
		reader.loadBeanDefinitions(new ClassPathResource("osgiNamespaceHandlerTests.xml", getClass()));
		appContext.refresh();
	}

	public void testSimpleReference() throws Exception {
		Object service = new Object();
		bundleCtxCtrl.expectAndReturn(bundleContext.getService(mockReference), service);

		bundleCtxCtrl.replay();
		Object factoryBean = appContext.getBean("&serializable");

		assertTrue(factoryBean instanceof OsgiServiceProxyFactoryBean);
		OsgiServiceProxyFactoryBean proxyFactory = (OsgiServiceProxyFactoryBean) factoryBean;
		assertSame(Serializable.class, proxyFactory.getInterface());

		// get the factory product
		Object bean = appContext.getBean("serializable");
		assertTrue(bean instanceof Serializable);
		assertTrue(Proxy.isProxyClass(bean.getClass()));

		bundleCtxCtrl.verify();
	}
}
