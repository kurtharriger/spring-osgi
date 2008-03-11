/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.osgi.iandt.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.osgi.iandt.BaseIntegrationTest;
import org.springframework.util.CollectionUtils;

public class BaseWebIntegrationTest extends BaseIntegrationTest {

	protected static final String WEB_TESTS_GROUP = "org.springframework.osgi.iandt.web";


	protected String[] getTestFrameworkBundlesNames() {
		String[] def = super.getTestFrameworkBundlesNames();
		List col = new ArrayList();
		CollectionUtils.mergeArrayIntoCollection(def, col);
		// add compendium API (from equinox since the OSGi one is not a bundle ...)
		// col.add("org.eclipse.bundles, org.eclipse.osgi.services, 20070605");

		// add Config Admin (from Felix)
		// col.add("org.apache.felix, org.apache.felix.configadmin, 1.0.0");

		// cm support
		// col.add("org.knopflerfish.bundles, cm_all, 2.0.0");
		// add http service (from equinox)
		//		col.add("org.eclipse.bundles, org.eclipse.equinox.supplement, 20070507");
		//		col.add("org.eclipse.bundles, org.mortbay.jetty, 20070611");
		//		col.add("org.eclipse.bundles, org.eclipse.equinox.http.jetty, 20070816");
		//		col.add("org.eclipse.bundles, org.eclipse.equinox.http.servlet, 20070816");
		//col.add("org.eclipse.bundles, org.eclipse.equinox.http, 20070423");
		//

		//System.setProperty("DEBUG", "true");
		//System.setProperty("VERBOSE", "true");

		// FIXME: can the creation of the logging folder be optional ?
		try {
			File logs = new File(".", "logs");
			if (!logs.exists())
				logs.mkdir();
			String path = logs.getCanonicalPath();
			System.setProperty("jetty.logs", path);
			logger.info("Jetty logging folder " + path);
		}
		catch (IOException ex) {
			logger.error("Ccannot create logging folder", ex);
		}
		// Servlet/JSP artifacts
		col.add("org.springframework.osgi, servlet-api.osgi, 2.5-SNAPSHOT");
		col.add("org.springframework.osgi, jsp-api.osgi, 2.0-SNAPSHOT");

		// JSP compiler
		col.add("org.springframework.osgi, ant.osgi, 1.7.0-SNAPSHOT");
		col.add("org.springframework.osgi, jsp-api.osgi, 2.0-SNAPSHOT");
		col.add("org.springframework.osgi, jasper.osgi, 5.5.23-SNAPSHOT");
		col.add("org.springframework.osgi, commons-el.osgi, 1.0-SNAPSHOT");

		// Jetty server
		col.add("org.mortbay.jetty, jetty-util, 6.2-SNAPSHOT");
		col.add("org.mortbay.jetty, jetty, 6.2-SNAPSHOT");

		// jetty starter
		col.add("org.springframework.osgi, jetty.start.osgi, 6.1.7-SNAPSHOT");
		col.add("org.springframework.osgi, jetty.etc.osgi, 6.1.7-SNAPSHOT");

		// Spring DM web extender
		col.add("org.springframework.osgi, spring-osgi-web," + getSpringDMVersion());
		col.add("org.springframework.osgi, cglib-nodep.osgi, 2.1.3-SNAPSHOT");

		return (String[]) col.toArray(new String[col.size()]);
	}

	protected String[] getBundleContentPattern() {
		String pkg = getClass().getPackage().getName().replace('.', '/');
		String basePackage = BaseWebIntegrationTest.class.getPackage().getName().replace('.', '/').concat("/");
		String[] patterns = new String[] { BaseIntegrationTest.class.getName().replace('.', '/').concat(".class"),
			basePackage + "/*", pkg + "/**/*" };
		return patterns;
	}
}
