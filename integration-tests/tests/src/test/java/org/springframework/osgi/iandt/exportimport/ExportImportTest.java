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
package org.springframework.osgi.iandt.exportimport;

import java.util.List;

import org.springframework.osgi.iandt.BaseIntegrationTest;

/**
 * @author Costin Leau
 */
public class ExportImportTest extends BaseIntegrationTest {

	@Override
	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/osgi/iandt/exportimport/export-import.xml" };
	}

	public void testCollectionSize() throws Exception {
		List list = (List) applicationContext.getBean("list");
		assertEquals(2, list.size());
		assertEquals(2, Listener.bind);
	}
	
	public void testExportNA() throws Exception {
		applicationContext.getBean("export-na");
		System.out.println(Listener.unbind);
		assertEquals(1, Listener.unbind);		
	}
}
