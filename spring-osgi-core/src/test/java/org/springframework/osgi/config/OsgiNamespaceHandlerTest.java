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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.service.OsgiServiceExporter;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean;

/**
 * UnitTest for OSGi namespace handler
 * 
 * @author Costin Leau
 * 
 */
public class OsgiNamespaceHandlerTest extends TestCase {

	private GenericApplicationContext appContext;
	private BundleContext bundleContext;
	private Bundle bundle;

	ServiceReference mockReference;

	public void setUp() throws Exception {
		bundleContext = new MockBundleContext();
		bundle = bundleContext.getBundle();

		appContext = new GenericApplicationContext();
		appContext.getBeanFactory().addBeanPostProcessor(new BundleContextAwareProcessor(bundleContext));

		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		// reader.setEventListener(this.listener);
		reader.loadBeanDefinitions(new ClassPathResource("osgiNamespaceHandlerTests.xml", getClass()));
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

	public void testSimpleService() throws Exception {
		Object bean = appContext.getBean("string-service");
		assertSame(OsgiServiceExporter.class, bean.getClass());
		OsgiServiceExporter exporter = (OsgiServiceExporter) bean;
		assertTrue(Arrays.equals(new String[] { "java.io.Serializable" }, exporter.getInterfaces()));
		assertEquals("string", exporter.getRef());
	}

	public void testFullService() throws Exception {
		OsgiServiceExporter exporter = (OsgiServiceExporter) appContext.getBean("full-service");
		assertEquals("string", exporter.getRef());

		// TODO: multiple interfaces are not supported!!!
		// assertTrue(Arrays.equals(new String[] { Serializable.class.getName(),
		// Cloneable.class.getName() },
		// exporter.getInterfaces()));
		Properties prop = new Properties();
		prop.setProperty("foo", "bar");
		prop.setProperty("white", "horse");
		assertEquals(prop, exporter.getServiceProperties());
	}

	public void testUnsupportedTags() throws Exception {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		// reader.setEventListener(this.listener);
		try {
			reader.loadBeanDefinitions(new ClassPathResource("unsupportedNamespaceHandlerTests.xml", getClass()));
			fail("should have thrown exception");
		}
		catch (RuntimeException ex) {
			// it's expected
			assertSame(UnsupportedOperationException.class, ex.getCause().getClass());
		}

	}

}
