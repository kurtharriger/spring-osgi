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
package org.springframework.osgi.io;

import org.osgi.framework.Bundle;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * OSGi specific ResourceLoader implementation.
 * 
 * The loader resolves paths inside an OSGi bundle using the bundle entries for
 * resource loading for any unqualified resource string.
 * 
 * Also understands the "bundle:" resource prefix for explicit loading of
 * resources from the bundle. When the bundle prefix is used the target resource
 * must be contained within the bundle (or attached fragments), the classpath is
 * not searched.
 * 
 * @see org.osgi.framework.Bundle
 * @see org.springframework.osgi.io.OsgiBundleResource
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 * 
 */
public class OsgiBundleResourceLoader extends DefaultResourceLoader {

	private Bundle bundle;

	/**
	 * Creates a OSGi aware ResourceLoader using the given bundle.
	 * @param bundle
	 */
	public OsgiBundleResourceLoader(Bundle bundle) {
		this.bundle = bundle;
	}

	protected Resource getResourceByPath(String path) {
		Assert.notNull(path, "Path is required");
		return new OsgiBundleResource(this.bundle, path);
	}

	/**
	 * Implementation of getResource that delegates to the bundle for any
	 * unqualified resource reference or a reference starting with "bundle:"
	 */
	public Resource getResource(String location) {
		Assert.notNull(location, "location is required");
		return new OsgiBundleResource(bundle, location);
	}

}
