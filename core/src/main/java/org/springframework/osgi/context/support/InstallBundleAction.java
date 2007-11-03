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
package org.springframework.osgi.context.support;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Bundle} install action. It tries to use Spring {@link ResourceLoader}
 * infrastructure for resolving the bundle location, falling back to the OSGi
 * platform in case of an exception.
 * 
 * @see BundleContext#installBundle(String)
 * @see BundleContext#installBundle(String, java.io.InputStream)
 * 
 * @author Costin Leau
 */
public class InstallBundleAction implements BundleAction, BundleContextAware, ResourceLoaderAware, InitializingBean {

	private static final Log log = LogFactory.getLog(InstallBundleAction.class);

	private BundleContext bundleContext;

	private String location;

	private Resource resource;

	private ResourceLoader resourceLoader;

	public Bundle execute(Bundle bundle) {
		// no bundle -> install it
		if (bundle == null) {
			try {
				return installBundle();
			}
			catch (BundleException ex) {
				throw new IllegalStateException("cannot install bundle from location [" + location + "]");
			}
		}
		return bundle;
	}

	private Bundle installBundle() throws BundleException {
		Assert.hasText(location, "location paramter required when installing a bundle");

		// install bundle (default)
		log.info("Loading bundle from [" + location + "]");

		Bundle bundle = null;
		boolean installBasedOnLocation = (resource == null);

		if (!installBasedOnLocation) {
			InputStream stream = null;
			try {
				stream = resource.getInputStream();
			}
			catch (IOException ex) {
				// catch it since we fallback on normal install
				installBasedOnLocation = true;
			}
			if (!installBasedOnLocation)
				bundle = bundleContext.installBundle(location, stream);
		}

		if (installBasedOnLocation)
			bundle = bundleContext.installBundle(location);

		return bundle;
	}

	public void afterPropertiesSet() {
		if (StringUtils.hasText(location)) {
			if (resourceLoader != null)
				resource = resourceLoader.getResource(location);
		}
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Resource getResource() {
		return resource;
	}

}
