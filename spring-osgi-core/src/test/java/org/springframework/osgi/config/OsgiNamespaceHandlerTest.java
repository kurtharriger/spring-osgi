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
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Properties;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;
import org.springframework.osgi.context.support.OsgiPropertyPlaceholder;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.service.OsgiServiceExporter;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.service.TargetSourceLifecycleListener;

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

	public void testFullReference() throws Exception {
		OsgiServiceProxyFactoryBean factory = (OsgiServiceProxyFactoryBean) appContext.getBean("&full-options");
		TargetSourceLifecycleListener[] listeners = factory.getListeners();
		assertNotNull(listeners);
		assertEquals(3, listeners.length);

		assertEquals(0, DummyListener.BIND_CALLS);
		assertEquals(0, DummyListener.UNBIND_CALLS);

		listeners[1].bind(null, null);

		assertEquals(2, DummyListener.BIND_CALLS);

		listeners[1].unbind(null, null);
		assertEquals(2, DummyListener.UNBIND_CALLS);
	}

	public void testSimpleService() throws Exception {
		Object bean = appContext.getBean(OsgiServiceExporter.class.getName());
		assertSame(OsgiServiceExporter.class, bean.getClass());
		OsgiServiceExporter exporter = (OsgiServiceExporter) bean;
		assertTrue(Arrays.equals(new Class[] { Serializable.class }, exporter.getInterfaces()));
		assertEquals(appContext.getBean("string"), exporter.getTarget());
	}

	public void testFullService() throws Exception {
		OsgiServiceExporter exporter = (OsgiServiceExporter) appContext.getBean(OsgiServiceExporter.class.getName()
				+ "#1");
		assertEquals(appContext.getBean("string"), exporter.getTarget());

		assertTrue(Arrays.equals(new Class[] { Serializable.class, Cloneable.class }, exporter.getInterfaces()));
		Properties prop = new Properties();
		prop.setProperty("foo", "bar");
		prop.setProperty("white", "horse");
		assertEquals(prop, exporter.getServiceProperties());
	}

	public void testSimplePlaceholder() throws Exception {
		OsgiPropertyPlaceholder simple = (OsgiPropertyPlaceholder) appContext.getBean(OsgiPropertyPlaceholder.class.getName());
		assertEquals("com.xyz.myapp", simple.getPersistentId());
	}

	public void testAveragePlaceholder() throws Exception {
		OsgiPropertyPlaceholder average = (OsgiPropertyPlaceholder) appContext.getBean(OsgiPropertyPlaceholder.class.getName()
				+ "#1");
		assertEquals("com.xyz.myapp", average.getPersistentId());
		Properties[] props = (Properties[]) getField(average, PropertiesLoaderSupport.class, "localProperties");
		assertEquals(1, props.length);

		assertEquals(appContext.getBean("external-props"), props[0]);
	}

	public void testFullPlaceholder() throws Exception {
		OsgiPropertyPlaceholder full = (OsgiPropertyPlaceholder) appContext.getBean(OsgiPropertyPlaceholder.class.getName()
				+ "#2");
		assertEquals("com.xyz.myapp", full.getPersistentId());
		
		Properties[] props = (Properties[]) getField(full, PropertiesLoaderSupport.class, "localProperties");
		assertEquals(1, props.length);

		Properties correctProperties = new Properties();
		correctProperties.setProperty("rod", "johnson");
		correctProperties.setProperty("rick", "evans");

		assertEquals(correctProperties, props[0]);
	}

	private Object getField(Object target, Class clazz, String fieldName) throws Exception {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(target);
	}

}
