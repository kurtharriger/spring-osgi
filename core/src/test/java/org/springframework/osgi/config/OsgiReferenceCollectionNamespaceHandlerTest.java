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

import java.io.Externalizable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.osgi.TestUtils;
import org.springframework.osgi.internal.context.support.BundleContextAwareProcessor;
import org.springframework.osgi.internal.service.collection.OsgiServiceCollection;
import org.springframework.osgi.internal.service.collection.OsgiServiceList;
import org.springframework.osgi.internal.service.collection.OsgiServiceSet;
import org.springframework.osgi.internal.service.collection.OsgiServiceSortedList;
import org.springframework.osgi.internal.service.collection.OsgiServiceSortedSet;
import org.springframework.osgi.internal.service.collection.comparator.OsgiServiceReferenceComparator;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.osgi.service.importer.OsgiMultiServiceProxyFactoryBean;

/**
 * @author Costin Leau
 * 
 */
public class OsgiReferenceCollectionNamespaceHandlerTest extends TestCase {

	private GenericApplicationContext appContext;

	protected void setUp() throws Exception {
		// reset counter just to be sure
		DummyListener.BIND_CALLS = 0;
		DummyListener.UNBIND_CALLS = 0;

		DummyListenerServiceSignature.BIND_CALLS = 0;
		DummyListenerServiceSignature.UNBIND_CALLS = 0;

		DummyListenerServiceSignature2.BIND_CALLS = 0;
		DummyListenerServiceSignature2.UNBIND_CALLS = 0;

		BundleContext bundleContext = new MockBundleContext() {
			// service reference already registered
			public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
				return new ServiceReference[0];
			}
		};

		appContext = new GenericApplicationContext();
		appContext.getBeanFactory().addBeanPostProcessor(new BundleContextAwareProcessor(bundleContext));
		appContext.setClassLoader(getClass().getClassLoader());

		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		// reader.setEventListener(this.listener);
		reader.loadBeanDefinitions(new ClassPathResource("osgiReferenceCollectionNamespaceHandlerTests.xml", getClass()));
		appContext.refresh();
	}

	protected void tearDown() throws Exception {
		appContext.close();
	}

	public void tstSimpleCollection() {
		Object factoryBean = appContext.getBean("&simpleCollection");

		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);
		OsgiMultiServiceProxyFactoryBean proxyFactory = (OsgiMultiServiceProxyFactoryBean) factoryBean;

		Class[] intfs = getInterfaces(proxyFactory);
		assertEquals(1, intfs.length);
		assertSame(Serializable.class, intfs[0]);

		// get the factory product
		Object bean = appContext.getBean("simpleCollection");
		assertTrue(bean instanceof Collection);
		assertTrue(bean instanceof OsgiServiceCollection);
	}

	public void testSimpleList() {
		Object factoryBean = appContext.getBean("&simpleList");
		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);
		// get the factory product
		Object bean = appContext.getBean("simpleList");
		assertTrue(bean instanceof OsgiServiceList);
	}

	public void testSimpleSet() {
		Object factoryBean = appContext.getBean("&simpleSet");
		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);
		// get the factory product
		Object bean = appContext.getBean("simpleSet");
		assertTrue(bean instanceof OsgiServiceSet);
	}

	public void testImplicitSortedList() {
		Object factoryBean = appContext.getBean("&implicitSortedList");
		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);
		// get the factory product
		Object bean = appContext.getBean("implicitSortedList");
		assertTrue(bean instanceof OsgiServiceSortedList);

		assertSame(appContext.getBean("defaultComparator"), ((OsgiServiceSortedList) bean).comparator());
	}

	public void testImplicitSortedSet() {
		Object factoryBean = appContext.getBean("&implicitSortedSet");
		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);

		Object bean = appContext.getBean("implicitSortedSet");
		assertTrue(bean instanceof OsgiServiceSortedSet);

		assertSame(appContext.getBean("defaultComparator"), ((SortedSet) bean).comparator());
	}

	public void tstSortedSetWithComparator() {
		Object factoryBean = appContext.getBean("&sortedSetWithComparator");
		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);

		Object bean = appContext.getBean("sortedSetWithComparator");
		assertTrue(bean instanceof OsgiServiceSortedSet);

		assertSame(appContext.getBean("defaultComparator"), ((SortedSet) bean).comparator());

	}

	public void tstSimpleSortedList() {
		Object factoryBean = appContext.getBean("&simpleSortedList");
		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);

		Object bean = appContext.getBean("simpleSortedList");
		assertTrue(bean instanceof OsgiServiceSortedList);

		Class[] intfs = getInterfaces(factoryBean);
		assertTrue(Arrays.equals(new Class[] { Serializable.class }, intfs));
	}

	public void tstSimpleSortedSet() {
		Object factoryBean = appContext.getBean("&simpleSortedSet");
		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);

		Object bean = appContext.getBean("simpleSortedSet");
		assertTrue(bean instanceof OsgiServiceSortedSet);

		Class[] intfs = getInterfaces(factoryBean);
		assertTrue(Arrays.equals(new Class[] { Comparable.class }, intfs));
	}

	public void testSortedSetWithNaturalOrderingOnRefs() throws Exception {
		Object factoryBean = appContext.getBean("&sortedSetWithNaturalOrderingOnRefs");
		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);
		
		Comparator comp = getComparator(factoryBean);
		
		assertNotNull(comp);
		assertSame(OsgiServiceReferenceComparator.class, comp.getClass());

		Class[] intfs = getInterfaces(factoryBean);
		assertTrue(Arrays.equals(new Class[] { Externalizable.class }, intfs));

		TargetSourceLifecycleListener[] listeners = getListeners(factoryBean);
		assertEquals(2, listeners.length);

		Object bean = appContext.getBean("sortedSetWithNaturalOrderingOnRefs");
		assertTrue(bean instanceof OsgiServiceSortedSet);

	}

	public void testSortedListWithNaturalOrderingOnServs() throws Exception {
		Object factoryBean = appContext.getBean("&sortedListWithNaturalOrderingOnServs");
		assertTrue(factoryBean instanceof OsgiMultiServiceProxyFactoryBean);

		assertNull(getComparator(factoryBean));

		Object bean = appContext.getBean("sortedListWithNaturalOrderingOnServs");
		assertTrue(bean instanceof OsgiServiceSortedList);

		Class[] intfs = getInterfaces(factoryBean);
		assertTrue(Arrays.equals(new Class[] { Externalizable.class }, intfs));
	}

	private Class[] getInterfaces(Object proxy) {
		return (Class[]) TestUtils.getFieldValue(proxy, "interfaces");
	}

	private Comparator getComparator(Object proxy) {
		return (Comparator) TestUtils.getFieldValue(proxy, "comparator");
	}

	private TargetSourceLifecycleListener[] getListeners(Object proxy) {
		return (TargetSourceLifecycleListener[]) TestUtils.getFieldValue(proxy, "listeners");
	}
}
