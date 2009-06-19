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
package org.springframework.osgi.context.support;

import java.lang.reflect.Method;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * 
 * @author Costin Leau
 */
public class ConfigLocationsTest extends TestCase {

	private OsgiBundleXmlApplicationContext context;

	@Override
	protected void setUp() throws Exception {
		context = new OsgiBundleXmlApplicationContext();
	}

	@Override
	protected void tearDown() throws Exception {
		context = null;
	}

	public void testExpandConfigFolders() throws Exception {
		String[] cfgs = new String[] { "cnf/", "/cnf/" };
		context.setConfigLocations(cfgs);
		String[] returned =
				(String[]) invokeMethod("expandLocations", new Class[] { String[].class }, new Object[] { cfgs });
		System.out.println("returned " + Arrays.toString(returned));
		assertTrue(Arrays.equals(new String[] { "cnf/*.xml", "/cnf/*.xml" }, returned));
	}

	private Object invokeMethod(String name, Class[] types, Object[] args) {
		try {
			Method mt = context.getClass().getDeclaredMethod(name, types);
			mt.setAccessible(true);
			return mt.invoke(context, args);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
