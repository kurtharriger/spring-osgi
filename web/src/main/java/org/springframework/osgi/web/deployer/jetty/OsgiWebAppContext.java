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

package org.springframework.osgi.web.deployer.jetty;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;
import org.osgi.framework.Bundle;

/**
 * Simple extension of WebAppContext that hooks in the special Osgi Jetty
 * resource.
 * 
 * @author Costin Leau
 * 
 */
class OsgiWebAppContext extends WebAppContext {

	private static final String BUNDLE_PROTOCOL = "bundle";
	/** OSGi bundle */
	private Bundle bundle;


	public Resource newResource(String urlAsString) throws IOException {
		// try converting to an URL
		URL url;
		try {
			url = new URL(urlAsString);
		}
		catch (MalformedURLException ex) {
			// failure (this occurs when dealing with local file resources)
			// use default jetty handling
			return Resource.newResource(urlAsString);
		}

		return newResource(url);
	}

	public Resource newResource(URL url) throws IOException {
		// bail out fast
		if (url == null)
			return null;

		// hack to support jetty resources
		String protocol = url.getProtocol();
		// OSGi bundle
		if (protocol.startsWith(BUNDLE_PROTOCOL)) {
			return new BundleSpaceJettyResource(bundle, url.getPath());
		}
		else
			return Resource.newResource(url);

	}

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}
}
