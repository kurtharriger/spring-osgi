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
import java.util.Arrays;
import java.util.Properties;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.service.OsgiServiceFactoryBean;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.service.TargetSourceLifecycleListener;

/**
 * Integration test for osgi:reference namespace handler.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiReferenceNamespaceHandlerTest extends TestCase {

	private GenericApplicationContext appContext;

	private BundleContext bundleContext;

	protected void setUp() throws Exception {
		bundleContext = new MockBundleContext() {

			public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
				return new ServiceReference[] { new MockServiceReference(new String[] { Cloneable.class.getName() }) };
			}
		};

		appContext = new GenericApplicationContext();
		appContext.getBeanFactory().addBeanPostProcessor(new BundleContextAwareProcessor(bundleContext));

		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		// reader.setEventListener(this.listener);
		reader.loadBeanDefinitions(new ClassPathResource("osgiReferenceNamespaceHandlerTests.xml", getClass()));
		appContext.refresh();
	}

	public void testSimpleReference() throws Exception {
		Object factoryBean = appContext.getBean("&serializable");

		assertTrue(factoryBean instanceof OsgiServiceProxyFactoryBean);
		OsgiServiceProxyFactoryBean proxyFactory = (OsgiServiceProxyFactoryBean) factoryBean;
		assertSame(Serializable.class, proxyFactory.getInterface());

		// get the factory product
		Object bean = appContext.getBean("serializable");
		assertTrue(bean instanceof Serializable);
		assertTrue(Proxy.isProxyClass(bean.getClass()));

	}

	public void testFullReference() throws Exception {
		OsgiServiceProxyFactoryBean factory = (OsgiServiceProxyFactoryBean) appContext.getBean("&full-options");
		TargetSourceLifecycleListener[] listeners = factory.getListeners();
		assertNotNull(listeners);
		assertEquals(5, listeners.length);

		assertEquals(0, DummyListener.BIND_CALLS);
		assertEquals(0, DummyListener.UNBIND_CALLS);

		listeners[1].bind(null, null);

		assertEquals(2, DummyListener.BIND_CALLS);

		listeners[1].unbind(null, null);
		assertEquals(2, DummyListener.UNBIND_CALLS);

		listeners[3].bind(null, null);
		assertEquals(1, DummyListenerServiceSignature.BIND_CALLS);

		listeners[3].unbind(null, null);
		assertEquals(1, DummyListenerServiceSignature.UNBIND_CALLS);

		listeners[4].bind(null, null);
		assertEquals(1, DummyListenerServiceSignature2.BIND_CALLS);

		listeners[4].unbind(null, null);
		assertEquals(1, DummyListenerServiceSignature2.UNBIND_CALLS);
	}


}
