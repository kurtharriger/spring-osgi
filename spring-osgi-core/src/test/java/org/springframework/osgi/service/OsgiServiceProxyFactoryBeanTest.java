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
import org.easymock.ArgumentsMatcher;
import org.osgi.framework.*;
import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.osgi.mock.MockBundle;

/**
 * @author Adrian Colyer
 * @author Hal Hildebrand
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

	public void testAfterPropertiesSetNoBundle() throws Exception {
		try {
			this.serviceFactoryBean.afterPropertiesSet();
			fail("should have throw IllegalArgumentException since bundle context was not set");
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Required bundleContext property was not set", ex.getMessage());
		}
	}

	public void testAfterPropertiesSetNoServiceType() throws Exception {
		this.serviceFactoryBean.setBundleContext(this.bundleContext);
		try {
			this.serviceFactoryBean.afterPropertiesSet();
			fail("should have throw IllegalArgumentException since service type was not set");
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Required serviceType property was not set", ex.getMessage());
		}
	}

	public void testAfterPropertiesSetBadFilter() throws Exception {
		this.serviceFactoryBean.setBundleContext(this.bundleContext);
		this.serviceFactoryBean.setInterface(ApplicationContext.class);
		this.serviceFactoryBean.setFilter("this is not a valid filter expression");
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
		this.serviceFactoryBean.setFilter(filter);
        String fullFilter
                = "(&(beanName=myBean)(objectClass=org.springframework.osgi.service.OsgiServiceProxyFactoryBeanTest.MyServiceInterface))";
        this.bundleContext.addServiceListener(new MockServiceListener(), fullFilter);
        this.mockControl.setMatcher(new AddServiceListenerMatcher());
        this.bundleContext.getServiceReferences(MyServiceInterface.class.getCanonicalName(), fullFilter);
        this.mockControl.setMatcher(MockControl.EQUALS_MATCHER);
        ServiceReference ref = getServiceReference();
		this.mockControl.setReturnValue(new ServiceReference[] { ref });
		this.bundleContext.getService(ref);
		MyServiceInterface serviceObj = new MyServiceInterface() {};
		this.mockControl.setReturnValue(serviceObj);
        this.bundleContext.getBundle();
        this.mockControl.setReturnValue(new MockBundle());
		this.mockControl.replay();
		this.serviceFactoryBean.afterPropertiesSet();
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

    private class MockServiceListener implements AllServiceListener {
        public void serviceChanged(ServiceEvent serviceEvent) {
        }
    }

    private class AddServiceListenerMatcher implements ArgumentsMatcher {

        public boolean matches(Object[] objects, Object[] objects1) {
            return objects1[0] instanceof ServiceListener
                   && objects[1].equals(objects1[1]);
        }


        public String toString(Object[] objects) {
            StringBuffer buf = new StringBuffer();
            buf.append("[");
            for (int i = 0; i < objects.length; i++) {
                buf.append(objects[i]);
                if (i < (objects.length - 1)) {
                    buf.append(", ");
                }
            }
            buf.append("]");
            return buf.toString();
        }
    }
}
