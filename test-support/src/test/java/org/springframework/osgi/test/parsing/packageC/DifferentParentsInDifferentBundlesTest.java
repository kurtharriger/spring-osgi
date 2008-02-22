/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.test.parsing.packageC;

import java.lang.reflect.Field;
import java.util.jar.Manifest;

import junit.framework.TestCase;

import org.osgi.framework.Constants;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.test.parsing.packageA.BaseClassFromAnotherPackage;
import org.springframework.osgi.test.parsing.packageB.BaseClassFromAnotherPackageAndBundle;

/**
 * Integration that checks if the class hierarchy is properly parsed. Note this
 * test doesn't run in OSGi, it just invokes the bytecode parsing.
 * 
 * @author Costin Leau
 * 
 */
public class DifferentParentsInDifferentBundlesTest extends TestCase {

	public void testCheckBaseClassesHierarchy() throws Exception {
		// create class
		TestInDifferentPackageThenItsParents test = new TestInDifferentPackageThenItsParents() {
		};

		Field jarSettings = AbstractConfigurableBundleCreatorTests.class.getDeclaredField("jarSettings");
		// initialize settings
		jarSettings.setAccessible(true);
		jarSettings.set(null, test.getSettings());

		Manifest mf = test.getManifest();
		String value = mf.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);

		// System.out.println("import package value is " + value);
		// check parent package
		assertTrue("missing parent not considered", contains(value,
			BaseClassFromAnotherPackageAndBundle.class.getPackage().getName()));
		assertFalse("contained parent not considered", contains(value,
			BaseClassFromAnotherPackage.class.getPackage().getName()));
		// check present parent dependencies
		assertTrue("contained parent dependencies not considered", contains(value, "javax.imageio"));

	}

	private boolean contains(String text, String item) {
		return text.indexOf(item) > -1;
	}
}
