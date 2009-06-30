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
package org.springframework.osgi.iandt.importer;

import java.awt.Shape;
import java.awt.geom.Area;
import java.util.Date;
import java.util.List;

import org.springframework.osgi.iandt.BaseIntegrationTest;

/**
 * @author Costin Leau
 */
public class CollectionTest extends BaseIntegrationTest {

	@Override
	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/osgi/iandt/importer/collection.xml" };
	}

	public void testServiceReferenceCollection() throws Exception {
		List list = applicationContext.getBean("reference-list", List.class);
		assertEquals(0, list.size());

		Listener listener = applicationContext.getBean("listener", Listener.class);
		assertEquals(0, listener.bind.size());
		Shape shape = new Area();
		bundleContext.registerService(Shape.class.getName(), shape, null);
		System.out.println("List is " + list);
		assertEquals(1, listener.bind.size());
	}
}
