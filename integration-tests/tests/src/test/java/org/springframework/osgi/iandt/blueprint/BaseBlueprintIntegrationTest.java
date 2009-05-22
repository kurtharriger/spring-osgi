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

package org.springframework.osgi.iandt.blueprint;

import java.util.ArrayList;
import java.util.List;

import org.springframework.osgi.iandt.BaseIntegrationTest;
import org.springframework.util.ObjectUtils;

/**
 * Blueprint base bundle. Contains RFC 124 specific items.
 * 
 * @author Costin Leau
 * 
 */
public abstract class BaseBlueprintIntegrationTest extends BaseIntegrationTest {

	protected String[] getTestFrameworkBundlesNames() {
		String[] bundles = super.getTestFrameworkBundlesNames();
		String[] matches = new String[] { "4j", "asm", "osgi-test", "junit" };

		List<String> list = new ArrayList<String>(bundles.length);
		for (int i = 0; i < bundles.length; i++) {
			String bundle = bundles[i];
			for (int j = 0; j < matches.length; j++) {
				String match = matches[j];
				if (bundle.indexOf(match) > -1) {
					list.add(bundle);
				}
			}
		}
		System.out.println(ObjectUtils.nullSafeToString(bundles));

		// install event admin
		list.add("org.apache.felix, org.apache.felix.eventadmin, 1.0.0");

		// install the rfc 124 big bundle
		list.add("org.springframework.osgi.rfc124,api," + getSpringDMVersion());
		list.add("org.springframework.osgi.rfc124.ri,all," + getSpringDMVersion());

		System.out.println(list);
		return list.toArray(new String[list.size()]);
	}

	protected String[] getBundleContentPattern() {
		String pkg = getClass().getPackage().getName().replace('.', '/').concat("/");
		String[] patterns = new String[] { BaseIntegrationTest.class.getName().replace('.', '/').concat("*.class"),
				BaseBlueprintIntegrationTest.class.getName().replace('.', '/').concat("*.class"), pkg + "**/*" };
		return patterns;
	}
}