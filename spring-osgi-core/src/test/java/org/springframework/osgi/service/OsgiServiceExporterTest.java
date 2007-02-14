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
 * Created on 26-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

import java.util.Properties;

import junit.framework.TestCase;
 
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;
import org.easymock.internal.AlwaysMatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * @author Adrian Colyer
 * @author Hal Hildebrand
 * @author Alin Dreghiciu
 * @since 1.0
 */
public class OsgiServiceExporterTest extends TestCase {

	private OsgiServiceExporter exporter = new OsgiServiceExporter();
	private BeanFactory beanFactory;
	private MockControl beanFactoryControl;
	private BundleContext bundleContext;
	private MockControl bundleContextControl;
	private MockControl mockServiceRegistrationControl;

	protected void setUp() throws Exception {
		this.beanFactoryControl = MockControl.createControl(BeanFactory.class);
		this.beanFactory = (BeanFactory) this.beanFactoryControl.getMock();
		this.bundleContextControl = MockControl.createControl(BundleContext.class);
		this.bundleContext = (BundleContext) this.bundleContextControl.getMock();
	}

	public void testAfterPropertiesSetNoBeans() throws Exception {
		this.exporter.setBeanFactory(this.beanFactory);
		this.exporter.setBundleContext(this.bundleContext);
		this.bundleContextControl.replay();
		this.beanFactoryControl.replay();
		try {
			this.exporter.afterPropertiesSet();
			fail("Expecting IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testAfterPropertiesSetNoBundleContext() throws Exception {
		this.exporter.setBeanFactory(this.beanFactory);
		try {
			this.exporter.afterPropertiesSet();
			fail("Expecting IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testAfterPropertiesSetNoResolver() throws Exception {
		this.exporter.setBeanFactory(this.beanFactory);
		this.exporter.setBundleContext(this.bundleContext);
		this.exporter.setResolver(null);
		try {
			this.exporter.afterPropertiesSet();
			fail("Expecting IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testAfterPropertiesSetNoBeanFactory() throws Exception {
		try {
			this.exporter.afterPropertiesSet();
			fail("Expecting IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	// TODO: redo
	public void tstPublish() throws Exception {
		this.exporter.setBeanFactory(this.beanFactory);
		this.exporter.setBundleContext(this.bundleContext);
		MockControl mc = MockControl.createControl(OsgiServicePropertiesResolver.class);
		OsgiServicePropertiesResolver resolver = (OsgiServicePropertiesResolver) mc.getMock();
		this.exporter.setResolver(resolver);
		Object target = new Object();
		this.exporter.setTarget(target);

		String beanName = OsgiServiceExporter.class.getName();
		// set expectations on afterProperties
		this.beanFactory.containsBean("&" + beanName);
		this.beanFactoryControl.setReturnValue(false);
		
		//this.beanFactory.getBean(beanName);
		//Object thisBean = new Object();
		//this.beanFactoryControl.setReturnValue(thisBean);

		resolver.getServiceProperties(beanName);
		mc.setReturnValue(new Properties());

		this.bundleContext.registerService((String[]) null, null, null);
		this.bundleContextControl.setMatcher(new AlwaysMatcher());
		this.bundleContextControl.setReturnValue(getServiceRegistration());

		this.bundleContextControl.replay();
		this.beanFactoryControl.replay();
		mc.replay();

		// do the work
		this.exporter.afterPropertiesSet();

		// verify
		this.bundleContextControl.verify();
		this.beanFactoryControl.verify();
		mc.verify();
	}

	// TODO: fix
	public void tstDestroy() throws Exception {
		tstPublish();
		this.mockServiceRegistrationControl.replay();
		this.exporter.destroy();
		this.mockServiceRegistrationControl.verify();
	}

	private ServiceRegistration getServiceRegistration() {
		this.mockServiceRegistrationControl = MockControl.createControl(ServiceRegistration.class);
		ServiceRegistration ret = (ServiceRegistration) this.mockServiceRegistrationControl.getMock();
		ret.unregister(); // for destroy test..
		return ret;
	}
	
	/**
	 * Test published service in case of a bean target service that is an 
	 * ServiceFactory and the bean is lazy initialized.
	 */
	public void testPublishServiceFactory() throws Exception {
		exporter.setBundleContext(bundleContext);
		// configure a BeanDefinitionRegistry Bean Factory
		MockControl registryCtrl = MockControl.createControl(MockBeanDefinitionRegistry.class);
		MockBeanDefinitionRegistry registry = (MockBeanDefinitionRegistry) registryCtrl.getMock();
		exporter.setBeanFactory(registry);
		// configure a BeanDefinition
		MockControl beanDefCtrl = MockControl.createControl(BeanDefinition.class);
		BeanDefinition beanDef = (BeanDefinition) beanDefCtrl.getMock();		
		// configure a mock property resolver
		MockControl resolverCtrl = MockControl.createControl(OsgiServicePropertiesResolver.class);
		OsgiServicePropertiesResolver resolver = (OsgiServicePropertiesResolver) resolverCtrl.getMock();
		exporter.setResolver(resolver);
		// configure a mock ServiceFactory
		MockControl targetCtrl = MockControl.createControl(ServiceFactory.class);
		ServiceFactory target = (ServiceFactory) targetCtrl.getMock();
		exporter.setTarget(target);
		// a dummy target service		
		String targetService = "targetService";
		// configure target bean name
		String beanName = "testServiceFactory";
		exporter.setTargetBeanName(beanName);
		// configure a mock ServiceRegistration
		MockControl registrationCtrl = MockControl.createControl(ServiceRegistration.class);
		ServiceRegistration registration = (ServiceRegistration) registrationCtrl.getMock();		
		
		// expected scenario
		registry.containsBean(BeanFactory.FACTORY_BEAN_PREFIX + beanName);
		registryCtrl.setReturnValue(false);
		registry.getBeanDefinition(beanName);
		registryCtrl.setReturnValue(beanDef);
		registry.getBean(beanName);
		registryCtrl.setReturnValue(target);
		beanDef.isLazyInit();
		beanDefCtrl.setReturnValue(true);
		resolver.getServiceProperties(beanName);
		resolverCtrl.setReturnValue(new Properties());
		
		bundleContext.registerService((String[]) null, null, null);
		RegisterServiceMatcher matcher = new RegisterServiceMatcher();
		bundleContextControl.setMatcher(matcher);
		bundleContextControl.setReturnValue(registration);
		target.getService(null, null);
		targetCtrl.setReturnValue(targetService);
		target.ungetService(null, null, targetService);
		
		bundleContextControl.replay();
		registryCtrl.replay();
		beanDefCtrl.replay();
		resolverCtrl.replay();
		targetCtrl.replay();
		registrationCtrl.replay();
		
		// play scenario
		// service registration
		exporter.afterPropertiesSet();
		assertNotNull("Registered service", matcher.serviceFactory);
		// parameters does not matter for now
		Object actualService = matcher.serviceFactory.getService(null, null);
		// we expect a dummy string as a service
		assertEquals(targetService, actualService);
		// and unregister silently 
		matcher.serviceFactory.ungetService(null, null, actualService);
		
		// verify scenario
		this.bundleContextControl.verify();
		registryCtrl.verify();
		beanDefCtrl.verify();
		resolverCtrl.verify();
		targetCtrl.verify();
		registrationCtrl.verify();
	}	
	
	/**
	 * Mock interface for an bean definition registry bean factory.
	 * @author Alin Dreghiciu
	 */
	private interface MockBeanDefinitionRegistry
		extends BeanFactory, BeanDefinitionRegistry {
	}
	
	/**
	 * Specific mather for service registration.
	 * It grabs the service registration to allow the test to call the actual
	 * service get. 
	 * 
	 * @author Alin Dreghiciu
	 */
	private static class RegisterServiceMatcher implements ArgumentsMatcher {
		
		public ServiceFactory serviceFactory; 

		public boolean matches(Object[] expected, Object[] actual) {
			if (actual != null 
				&& actual.length == 3
				&& actual[1] != null
				&& actual[1] instanceof ServiceFactory) {
				serviceFactory = (ServiceFactory) actual[1];
				return true;
			}
			return false;
		}

		public String toString(Object[] arguments) {
			if (arguments == null)
			    arguments = new Object[0];
			StringBuffer result = new StringBuffer();
			for (int i = 0; i < arguments.length; i++) {
			    if (i > 0)
				result.append(", ");
			    result.append(argumentToString(arguments[i]));
			}
			return result.toString();
		}
		
	    private String argumentToString(Object argument) {
	    	if (argument instanceof String)
	    	    return "\"" + argument + "\"";
	    	return "" + argument;
	    }		
	}

}
