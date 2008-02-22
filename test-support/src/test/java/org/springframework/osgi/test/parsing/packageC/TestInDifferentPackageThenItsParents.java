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

import java.util.Properties;
import java.util.jar.Manifest;

import org.springframework.osgi.test.parsing.packageA.BaseClassFromAnotherPackage;
import org.springframework.osgi.test.parsing.packageB.BaseClassFromAnotherPackageAndBundle;

/**
 * @author Costin Leau
 * 
 */
public abstract class TestInDifferentPackageThenItsParents extends BaseClassFromAnotherPackageAndBundle {

	public void testCheckBaseClassesHierarchy() throws Exception {
		Manifest mf = getManifest();
		System.out.println(mf.getMainAttributes().entrySet());
	}

	protected String[] getBundleContentPattern() {
		String pkg = getClass().getPackage().getName().replace('.', '/').concat("/");
		String[] patterns = new String[] { pkg + "**/*",
			BaseClassFromAnotherPackage.class.getName().replace('.', '/').concat(".class") };
		return patterns;
	}

	protected String getRootPath() {
		return super.getRootPath();
	}

	protected Manifest getManifest() {
		return super.getManifest();
	}

	protected Properties getSettings() throws Exception {
		return super.getSettings();
	}

}
