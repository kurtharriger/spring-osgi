/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.osgi.blueprint.container;

import java.beans.PropertyDescriptor;

import junit.framework.TestCase;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;

/**
 * 
 * @author Costin Leau
 */
public class BlueprintFields extends TestCase {

	public void testUseBlueprintExceptions() throws Exception {
		OsgiServiceProxyFactoryBean fb = new OsgiServiceProxyFactoryBean();
		BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(fb);
		String propertyName = "useBlueprintExceptions";
		for (PropertyDescriptor desc : wrapper.getPropertyDescriptors()) {
			System.out.println(desc.getName());
		}
		Class type = wrapper.getPropertyType(propertyName);
		System.out.println("type is " + type);
		assertTrue(wrapper.isWritableProperty(propertyName));
		assertFalse(wrapper.isReadableProperty(propertyName));
	}
}
