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

import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * @author Adrian Colyer
 * @author Hal Hildebrand
 * @since 2.0
 */
public class BeanNameServicePropertiesResolverTest extends TestCase {


	public void testAfterPropertiesSetNoBundleContext() throws Exception {
		try {
			new BeanNameServicePropertiesResolver().afterPropertiesSet();
			fail( "Should have thrown IllegalArgumentException");
		} 
		catch (IllegalArgumentException ex) {
			// expected
		}
	}
	
	public void testGetServiceProperties() {
		MockControl bundleContextControl = MockControl.createControl(BundleContext.class);
		BundleContext mockContext = (BundleContext) bundleContextControl.getMock();
		MockControl bundleControl = MockControl.createControl(Bundle.class);
		Bundle mockBundle = (Bundle) bundleControl.getMock();
		
		mockContext.getBundle();
		bundleContextControl.setReturnValue(mockBundle);
		mockBundle.getSymbolicName();
		bundleControl.setReturnValue("symbolic-name");
		mockContext.getBundle();
		bundleContextControl.setReturnValue(mockBundle);
		mockBundle.getHeaders();
		Properties props = new Properties();
		props.put(Constants.BUNDLE_VERSION,"1.0.0");
		bundleControl.setReturnValue(props);
		
		bundleContextControl.replay();
		bundleControl.replay();
		
		BeanNameServicePropertiesResolver resolver = new BeanNameServicePropertiesResolver();
		resolver.setBundleContext(mockContext);
		Map ret = resolver.getServiceProperties("myBean");
		
		bundleControl.verify();
		bundleContextControl.verify();
		
		assertEquals("3 properties",3,ret.size());
		assertEquals("symbolic-name",ret.get("Bundle-SymbolicName"));
		assertEquals("1.0.0",ret.get("Bundle-Version"));
		assertEquals("myBean",ret.get("org.springframework.osgi.beanname"));
	}
	
}
