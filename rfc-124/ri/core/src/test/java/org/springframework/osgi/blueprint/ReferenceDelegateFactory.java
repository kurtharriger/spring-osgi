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

package org.springframework.osgi.blueprint;

import java.lang.reflect.Constructor;

import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.service.importer.ImportedOsgiServiceProxy;
import org.springframework.osgi.service.importer.ServiceReferenceProxy;

/**
 * @author Costin Leau
 */
public class ReferenceDelegateFactory implements FactoryBean {

	private final Class clazz;
	private final ServiceReference ref;


	public ReferenceDelegateFactory() throws Exception {
		clazz = Class.forName("org.springframework.osgi.service.importer.support.ServiceReferenceDelegate", true,
			getClass().getClassLoader());

		ref = new MockServiceReference();
	}

	public Object getObject() throws Exception {
		ImportedOsgiServiceProxy mockProxy = new ImportedOsgiServiceProxy() {

			public ServiceReferenceProxy getServiceReference() {
				return null;
			}
		};

		Constructor ctor = clazz.getDeclaredConstructor(ImportedOsgiServiceProxy.class);
		ctor.setAccessible(true);
		Object instance = ctor.newInstance(mockProxy);
		return instance;
	}

	public Class getObjectType() {
		return clazz;
	}

	public boolean isSingleton() {
		return false;
	}
}
