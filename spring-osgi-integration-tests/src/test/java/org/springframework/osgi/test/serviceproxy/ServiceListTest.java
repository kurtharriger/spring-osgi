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
package org.springframework.osgi.test.serviceproxy;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.springframework.osgi.service.OsgiServiceProxyFactoryBean.ReferenceClassLoadingOptions;
import org.springframework.osgi.service.collection.OsgiServiceList;

/**
 * @author Costin Leau
 * 
 */
public class ServiceListTest extends ServiceCollectionTest {

	protected Collection createCollection() {
		return new OsgiServiceList(null, null, getBundleContext(), ReferenceClassLoadingOptions.UNMANAGED);
	}

	public void testListContent() throws Exception {
		List list = (List) createCollection();

		// test the list iterator
		ListIterator iter = list.listIterator();
		Object b = iter.next();

		assertSame(b, iter.previous());
	}

}
