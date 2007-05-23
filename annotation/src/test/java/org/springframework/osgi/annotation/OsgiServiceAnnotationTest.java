/*
 * Copyright 2006 the original author or authors.
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
package org.springframework.osgi.annotation;

import java.lang.reflect.Method;
import java.io.Serializable;
import java.beans.PropertyDescriptor;
import java.util.Arrays;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.service.CardinalityOptions;
import org.springframework.osgi.service.exporter.ExportClassLoadingOptions;
import org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.service.importer.ReferenceClassLoadingOptions;

/**
 * @author Andy Piper
 */
public class OsgiServiceAnnotationTest extends TestCase {

	private ServiceReferenceInjectionBeanPostProcessor processor;
	private BundleContext context;

	protected void setUp() throws Exception {
		super.setUp();
		processor = new ServiceReferenceInjectionBeanPostProcessor();
		context = new MockBundleContext();
		processor.setBundleContext(context);
		MockControl factoryControl = MockControl.createControl(BeanFactory.class);
		BeanFactory factory = (BeanFactory) factoryControl.getMock();
		processor.setBeanFactory(factory);
	}

	protected void tearDown() throws Exception {
	}

	public void testGetServicePropertySetters() throws Exception {
		OsgiServiceProxyFactoryBean pfb = new OsgiServiceProxyFactoryBean();
		Method setter = AnnotatedBean.class.getMethod("setStringType", new Class[]{String.class});
		ServiceReference ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);

		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getInterface()[0], String.class);

		setter = AnnotatedBean.class.getMethod("setIntType", new Class[]{Integer.TYPE});
		ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);

		pfb = new OsgiServiceProxyFactoryBean();
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getInterface()[0], Integer.TYPE);

	}

	public void testGetServicePropertyCardinality() throws Exception {
		OsgiServiceProxyFactoryBean pfb = new OsgiServiceProxyFactoryBean();
		Method setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithCardinality1_1", new Class[]{AnnotatedBean.class});
		ServiceReference ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getCard(), CardinalityOptions.C_1__1);

		setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithCardinality0_1", new Class[]{AnnotatedBean.class});
		ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		pfb = new OsgiServiceProxyFactoryBean();
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getCard(), CardinalityOptions.C_0__1);

		setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithCardinality0_N", new Class[]{AnnotatedBean.class});
		ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		pfb = new OsgiServiceProxyFactoryBean();
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getCard(), CardinalityOptions.C_0__N);

		setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithCardinality1_N", new Class[]{AnnotatedBean.class});
		ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		pfb = new OsgiServiceProxyFactoryBean();
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getCard(), CardinalityOptions.C_1__N);
	}

	public void testGetServicePropertyClassloader() throws Exception {
		OsgiServiceProxyFactoryBean pfb = new OsgiServiceProxyFactoryBean();
		Method setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithClassLoaderClient", new Class[]{AnnotatedBean.class});
		ServiceReference ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getContextCL(), ReferenceClassLoadingOptions.CLIENT);

		pfb = new OsgiServiceProxyFactoryBean();
		setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithClassLoaderUmanaged", new Class[]{AnnotatedBean.class});
		ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getContextCL(), ExportClassLoadingOptions.UNMANAGED);

		pfb = new OsgiServiceProxyFactoryBean();
		setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithClassLoaderServiceProvider", new Class[]{AnnotatedBean.class});
		ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getContextCL(), ExportClassLoadingOptions.SERVICE_PROVIDER);
	}

	public void testGetServicePropertyBeanName() throws Exception {
		OsgiServiceProxyFactoryBean pfb = new OsgiServiceProxyFactoryBean();
		Method setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithBeanName", new Class[]{AnnotatedBean.class});
		ServiceReference ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getServiceBeanName(), "myBean");
	}

	public void testGetServicePropertyFilter() throws Exception {
		OsgiServiceProxyFactoryBean pfb = new OsgiServiceProxyFactoryBean();
		Method setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithFilter", new Class[]{AnnotatedBean.class});
		ServiceReference ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getFilter(), "(wooey=fooo)");
	}

	public void testGetServicePropertyServiceClass() throws Exception {
		OsgiServiceProxyFactoryBean pfb = new OsgiServiceProxyFactoryBean();
		Method setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeWithServiceType", new Class[]{AnnotatedBean.class});
		ServiceReference ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getInterface()[0], Object.class);
	}

	public void testGetServicePropertyComplex() throws Exception {
		OsgiServiceProxyFactoryBean pfb = new OsgiServiceProxyFactoryBean();
		Method setter = AnnotatedBean.class.getMethod("setAnnotatedBeanTypeComplex", new Class[]{AnnotatedBean.class});
		ServiceReference ref = AnnotationUtils.getAnnotation(setter, ServiceReference.class);
		processor.getServiceProperty(pfb, ref, setter, null);
		assertEquals(pfb.getInterface()[0], AnnotatedBean.class);
		assertEquals(pfb.getCard(), CardinalityOptions.C_0__N);
		assertEquals(pfb.getContextCL(), ExportClassLoadingOptions.SERVICE_PROVIDER);
		assertEquals(pfb.getFilter(), "(id=fooey)");
		assertEquals(pfb.getServiceBeanName(), "myBean");
	}

	public void testServiceBeanInjection() throws Exception {
		ServiceBean bean = new ServiceBean();
		final MyService bean1 = new MyService() {
			public Object getId() {
				return this;
			}
		};
		final Serializable bean2 = new Serializable() {
			public String toString() { return "bean2"; }
		};

		BundleContext context = new MockBundleContext() {
			public Object getService(org.osgi.framework.ServiceReference reference) {
				String clazz = ((String[])reference.getProperty(Constants.OBJECTCLASS))[0];
				if (clazz == null) return null;
				else if (clazz.equals(MyService.class.getName())) {
					return bean1;
				}
				else if (clazz.equals(Serializable.class.getName())) {
					return bean2;
				}
				return null;
			}

		};

		ServiceReferenceInjectionBeanPostProcessor p = new ServiceReferenceInjectionBeanPostProcessor();
		p.setBundleContext(context);
		MockControl factoryControl = MockControl.createControl(BeanFactory.class);
		BeanFactory factory = (BeanFactory) factoryControl.getMock();
		factoryControl.expectAndReturn(factory.containsBean("&myBean"), true);
		factoryControl.replay();
		p.setBeanFactory(factory);

		p.postProcessAfterInitialization(bean, "myBean");
		assertSame(bean1.getId(), bean.getServiceBean().getId());
		assertSame(bean2.toString(), bean.getSerializableBean().toString());

		factoryControl.verify();
	}

	public void testServiceFactoryBeanNotInjected() throws Exception {
		ServiceFactoryBean bean = new ServiceFactoryBean();
		final MyService bean1 = new MyService() {
			public Object getId() {
				return this;
			}
		};
		final Serializable bean2 = new Serializable() {
			public String toString() { return "bean2"; }
		};

		BundleContext context = new MockBundleContext() {
			public Object getService(org.osgi.framework.ServiceReference reference) {
				String clazz = ((String[])reference.getProperty(Constants.OBJECTCLASS))[0];
				if (clazz == null) return null;
				else if (clazz.equals(MyService.class.getName())) {
					return bean1;
				}
				else if (clazz.equals(Serializable.class.getName())) {
					return bean2;
				}
				return null;
			}

		};

		ServiceReferenceInjectionBeanPostProcessor p = new ServiceReferenceInjectionBeanPostProcessor();
		p.setBundleContext(context);
		p.postProcessAfterInitialization(bean, "myBean");
		assertNull(bean.getServiceBean());
		assertNull(bean.getSerializableBean());
	}

	public void testServiceFactoryBeanInjected() throws Exception {
		ServiceFactoryBean bean = new ServiceFactoryBean();
		final MyService bean1 = new MyService() {
			public Object getId() {
				return this;
			}
		};
		final Serializable bean2 = new Serializable() {
			public String toString() { return "bean2"; }
		};

		BundleContext context = new MockBundleContext() {
			public Object getService(org.osgi.framework.ServiceReference reference) {
				String clazz = ((String[])reference.getProperty(Constants.OBJECTCLASS))[0];
				if (clazz == null) return null;
				else if (clazz.equals(MyService.class.getName())) {
					return bean1;
				}
				else if (clazz.equals(Serializable.class.getName())) {
					return bean2;
				}
				return null;
			}

		};

		ServiceReferenceInjectionBeanPostProcessor p = new ServiceReferenceInjectionBeanPostProcessor();
		p.setBundleContext(context);
		PropertyValues pvs = p.postProcessPropertyValues(new MutablePropertyValues(),
			new PropertyDescriptor[] {
				new PropertyDescriptor("serviceBean", ServiceFactoryBean.class),
				new PropertyDescriptor("serializableBean", ServiceFactoryBean.class) }, bean, "myBean");

		MyService msb = (MyService)pvs.getPropertyValue("serviceBean").getValue();
		Serializable ssb = (Serializable)pvs.getPropertyValue("serializableBean").getValue();

		assertNotNull(msb);
		assertNotNull(ssb);

		assertSame(bean1.getId(), msb.getId());
		assertSame(bean2.toString(), ssb.toString());
	}

	public void testServiceBeanInjectedValues() throws Exception {
		ServiceBean bean = new ServiceBean();
		final MyService bean1 = new MyService() {
			public Object getId() {
				return this;
			}
		};
		final Serializable bean2 = new Serializable() {
			public String toString() { return "bean2"; }
		};

		BundleContext context = new MockBundleContext() {
			public Object getService(org.osgi.framework.ServiceReference reference) {
				String clazz = ((String[])reference.getProperty(Constants.OBJECTCLASS))[0];
				if (clazz == null) return null;
				else if (clazz.equals(MyService.class.getName())) {
					return bean1;
				}
				else if (clazz.equals(Serializable.class.getName())) {
					return bean2;
				}
				return null;
			}

		};

		ServiceReferenceInjectionBeanPostProcessor p = new ServiceReferenceInjectionBeanPostProcessor();
		p.setBundleContext(context);
		PropertyValues pvs = p.postProcessPropertyValues(new MutablePropertyValues(),
			new PropertyDescriptor[] {
				new PropertyDescriptor("serviceBean", ServiceBean.class),
				new PropertyDescriptor("serializableBean", ServiceBean.class) }, bean, "myBean");

		MyService msb = (MyService)pvs.getPropertyValue("serviceBean").getValue();
		Serializable ssb = (Serializable)pvs.getPropertyValue("serializableBean").getValue();

		assertNotNull(msb);
		assertNotNull(ssb);

		assertSame(bean1.getId(), msb.getId());
		assertSame(bean2.toString(), ssb.toString());
	}
}
