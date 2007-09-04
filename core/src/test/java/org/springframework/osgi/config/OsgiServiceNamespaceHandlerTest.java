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
package org.springframework.osgi.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.TestUtils;
import org.springframework.osgi.context.support.BundleContextAwareProcessor;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.mock.MockServiceRegistration;
import org.springframework.osgi.service.exporter.OsgiServiceFactoryBean;

/**
 * Integration test for osgi:service namespace handler.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceNamespaceHandlerTest extends TestCase {

	private GenericApplicationContext appContext;

	private BundleContext bundleContext;

	private final List services = new ArrayList();

	private ServiceRegistration registration;

	protected void setUp() throws Exception {

		services.clear();

		registration = new MockServiceRegistration();

		bundleContext = new MockBundleContext() {

			public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
				return new ServiceReference[] { new MockServiceReference(new String[] { Cloneable.class.getName() }) };
			}

			public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
				services.add(service);
				return registration;
			}
		};

		appContext = new GenericApplicationContext();
		appContext.getBeanFactory().addBeanPostProcessor(new BundleContextAwareProcessor(bundleContext));

		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		// reader.setEventListener(this.listener);
		reader.loadBeanDefinitions(new ClassPathResource("osgiServiceNamespaceHandlerTests.xml", getClass()));
		appContext.refresh();
	}

	private Object getServiceAtIndex(int index) {
		Object sFactory = services.get(index);
		assertNotNull(sFactory);
		assertTrue(sFactory instanceof ServiceFactory);
		ServiceFactory fact = (ServiceFactory) sFactory;
		return fact.getService(null, null);
	}

	public void testSimpleService() throws Exception {
		Object bean = appContext.getBean("&inlineReference");
		assertSame(OsgiServiceFactoryBean.class, bean.getClass());
		OsgiServiceFactoryBean exporter = (OsgiServiceFactoryBean) bean;

		assertTrue(Arrays.equals(new Class[] { Serializable.class }, getInterfaces(exporter)));
		assertEquals("string", getTargetBeanName(exporter));
		assertEquals(appContext.getBean("string"), getTarget(exporter));

		assertSame(appContext.getBean("string"), getServiceAtIndex(0));
	}

	public void testBiggerService() throws Exception {
		OsgiServiceFactoryBean exporter = (OsgiServiceFactoryBean) appContext.getBean("&manyOptions");

		assertTrue(Arrays.equals(new Class[] { Serializable.class, Cloneable.class }, getInterfaces(exporter)));
		Properties prop = new Properties();
		prop.setProperty("foo", "bar");
		prop.setProperty("white", "horse");
		assertEquals(prop, exporter.getServiceProperties());

		// Should be wrapped with a TCCL setting proxy
		assertNotSame(appContext.getBean("string"), getServiceAtIndex(1));

		assertEquals("string", getTargetBeanName(exporter));
		assertEquals(appContext.getBean("string"), getTarget(exporter));

	}

	public void testNestedService() throws Exception {
		OsgiServiceFactoryBean exporter = (OsgiServiceFactoryBean) appContext.getBean("&nestedService");
		assertTrue(Arrays.equals(new Class[] { Object.class }, getInterfaces(exporter)));

		Object service = getServiceAtIndex(2);
		assertSame(HashMap.class, service.getClass());

		assertNull(getTargetBeanName(exporter));
		assertNotNull(getTarget(exporter));
	}

	public void testServiceExporterFactoryBean() throws Exception {
		Object bean = appContext.getBean("nestedService");
		assertTrue(bean instanceof ServiceRegistration);
		assertNotSame("registration not wrapped to provide exporting listener notification", registration, bean);
	}

	public void testServiceProperties() throws Exception {
		OsgiServiceFactoryBean exporter = (OsgiServiceFactoryBean) appContext.getBean("&serviceProperties");
		Map properties = exporter.getServiceProperties();
		assertEquals(2, properties.size());
		assertTrue(properties.get("string") instanceof String);
		assertTrue(properties.get("int") instanceof Integer);

		assertNull(getTargetBeanName(exporter));
		assertNotNull(getTarget(exporter));

	}

	private Class[] getInterfaces(OsgiServiceFactoryBean exporter) {
		return (Class[]) TestUtils.getFieldValue(exporter, "interfaces");
	}

	private String getTargetBeanName(OsgiServiceFactoryBean exporter) {
		return (String) TestUtils.getFieldValue(exporter, "targetBeanName");
	}

	private Object getTarget(OsgiServiceFactoryBean exporter) {
		return TestUtils.getFieldValue(exporter, "target");
	}
}
