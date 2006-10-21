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
 * Created on 25-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.easymock.internal.AlwaysMatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
public class OsgiServiceProxyFactoryBeanTest extends TestCase {

	private OsgiServiceProxyFactoryBean serviceFactoryBean;
	private MockControl mockControl;
	private BundleContext bundleContext;

	protected void setUp() throws Exception {
		super.setUp();
		this.serviceFactoryBean = new OsgiServiceProxyFactoryBean();
		this.serviceFactoryBean.setApplicationContext(new GenericApplicationContext());
		this.mockControl = MockControl.createControl(BundleContext.class);
		this.bundleContext = (BundleContext) this.mockControl.getMock();
	}

	public void testAfterPropertiesSetNoBundle() {
		try {
			this.serviceFactoryBean.afterPropertiesSet();
			fail("should have throw IllegalArgumentException since bundle context was not set");
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Required bundleContext property was not set", ex.getMessage());
		}
	}

	public void testAfterPropertiesSetNoServiceType() {
		this.serviceFactoryBean.setBundleContext(this.bundleContext);
		try {
			this.serviceFactoryBean.afterPropertiesSet();
			fail("should have throw IllegalArgumentException since service type was not set");
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Required serviceType property was not set", ex.getMessage());
		}
	}

	public void testAfterPropertiesSetBadFilter() {
		this.serviceFactoryBean.setBundleContext(this.bundleContext);
		this.serviceFactoryBean.setInterface(ApplicationContext.class);
		this.serviceFactoryBean.setTarget("this is not a valid filter expression");
		try {
			this.serviceFactoryBean.afterPropertiesSet();
			fail("should have throw IllegalArgumentException since filter has invalid syntax");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(
					"message should say that filter string blah is not valid",
					ex.getMessage().startsWith(
							"Filter string 'this is not a valid filter expression' set on OsgiServiceProxyFactoryBean has invalid syntax:"));
		}
	}

	public void testGetObjectType() {
		this.serviceFactoryBean.setInterface(ApplicationContext.class);
		assertEquals(ApplicationContext.class, this.serviceFactoryBean.getObjectType());
	}

	public void testGetObjectWithFilterOnly() throws Exception {
		// OsgiServiceUtils are tested independently in error cases, here we
		// test the
		// correct behaviour of the ProxyFactoryBean when OsgiServiceUtils
		// succesfully
		// finds the service.
		this.serviceFactoryBean.setBundleContext(this.bundleContext);
		this.serviceFactoryBean.setInterface(MyServiceInterface.class);
		String filter = "(beanName=myBean)";
		this.serviceFactoryBean.setTarget(filter);
		this.serviceFactoryBean.afterPropertiesSet();
		this.bundleContext.getServiceReferences(MyServiceInterface.class.getName(), filter);
		ServiceReference ref = getServiceReference();
		this.mockControl.setReturnValue(new ServiceReference[] { ref });
		this.bundleContext.getService(ref);
		MyServiceInterface serviceObj = new MyServiceInterface() {
		};
		this.mockControl.setReturnValue(serviceObj);
		this.bundleContext.addServiceListener(null);
		this.mockControl.setMatcher(new AlwaysMatcher());
		this.mockControl.replay();
		MyServiceInterface s = (MyServiceInterface) this.serviceFactoryBean.getObject();
		assertTrue("s should be proxied", s instanceof Advised);
		assertSame("proxy target should be the service", serviceObj, ((Advised) s).getTargetSource().getTarget());
		this.mockControl.verify();
	}

	public void testGetObjectWithBeanNameOnly() throws Exception {
		// OsgiServiceUtils are tested independently in error cases, here we
		// test the
		// correct behaviour of the ProxyFactoryBean when OsgiServiceUtils
		// succesfully
		// finds the service.
		this.serviceFactoryBean.setBundleContext(this.bundleContext);
		this.serviceFactoryBean.setInterface(MyServiceInterface.class);
		this.serviceFactoryBean.setBeanName("myBean");
		this.serviceFactoryBean.afterPropertiesSet();
		this.bundleContext.getServiceReferences(MyServiceInterface.class.getName(),
				"(org.springframework.osgi.beanname=myBean)");
		ServiceReference ref = getServiceReference();
		this.mockControl.setReturnValue(new ServiceReference[] { ref });
		this.bundleContext.getService(ref);
		MyServiceInterface serviceObj = new MyServiceInterface() {
		};
		this.mockControl.setReturnValue(serviceObj);
		this.bundleContext.addServiceListener(null);
		this.mockControl.setMatcher(new AlwaysMatcher());
		this.mockControl.replay();
		MyServiceInterface s = (MyServiceInterface) this.serviceFactoryBean.getObject();
		assertTrue("s should be proxied", s instanceof Advised);
		assertSame("proxy target should be the service", serviceObj, ((Advised) s).getTargetSource().getTarget());
		this.mockControl.verify();
	}


	public void testGetObjectWithFilterAndBeanName() throws Exception {
		// OsgiServiceUtils are tested independently in error cases, here we
		// test the
		// correct behaviour of the ProxyFactoryBean when OsgiServiceUtils
		// succesfully
		// finds the service.
		this.serviceFactoryBean.setBundleContext(this.bundleContext);
		this.serviceFactoryBean.setInterface(MyServiceInterface.class);
		this.serviceFactoryBean.setTarget("(xyz=*)");
		this.serviceFactoryBean.setBeanName("myBean");
		this.serviceFactoryBean.afterPropertiesSet();
		this.bundleContext.getServiceReferences(MyServiceInterface.class.getName(),
				"(&(xyz=*)(org.springframework.osgi.beanname=myBean))");
		ServiceReference ref = getServiceReference();
		this.mockControl.setReturnValue(new ServiceReference[] { ref });
		this.bundleContext.getService(ref);
		MyServiceInterface serviceObj = new MyServiceInterface() {
		};
		this.mockControl.setReturnValue(serviceObj);
		this.bundleContext.addServiceListener(null);
		this.mockControl.setMatcher(new AlwaysMatcher());
		this.mockControl.replay();
		MyServiceInterface s = (MyServiceInterface) this.serviceFactoryBean.getObject();
		assertTrue("s should be proxied", s instanceof Advised);
		assertSame("proxy target should be the service", serviceObj, ((Advised) s).getTargetSource().getTarget());
		this.mockControl.verify();
	}

	private ServiceReference getServiceReference() {
		MockControl sRefControl = MockControl.createNiceControl(ServiceReference.class);
		return (ServiceReference) sRefControl.getMock();
	}

	public interface MyServiceInterface {
	}

	public void testClassLoadingOptionsConstant() throws Exception {
		serviceFactoryBean.setContextClassloader("client");
		serviceFactoryBean.setContextClassloader("service-provider");
		serviceFactoryBean.setContextClassloader("unmanaged");
	}

	public void testCardinalityConstant() throws Exception {
		serviceFactoryBean.setCardinality("0..1");
		serviceFactoryBean.setCardinality("0..n");
		serviceFactoryBean.setCardinality("1..1");
		serviceFactoryBean.setCardinality("1..n");

		try {
			serviceFactoryBean.setCardinality("bogus");
			fail("should have thrown exception ");
		}
		catch (Exception ex) {
			// expected
		}
	}
}
