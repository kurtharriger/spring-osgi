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

package org.springframework.osgi.web.extender.deployer.support;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.springframework.osgi.web.extender.deployer.ContextPathStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ContextPathStrategy} default implementation that takes into account
 * the OSGi bundle properties for determining the war context path.
 * 
 * <p/>This implementation iterates through following properties, considering
 * the first one that is available in the following order:
 * 
 * <ol>
 * <li>bundle location - if present, the location filename is returned as the
 * context path.</li>
 * <li>bundle name - if present, it is used as a fall back to the bundle
 * location.</li>
 * <li>bundle symbolic name - if present, it used as a fallback to the bundle
 * name.</li>
 * <li>bundle identity - if neither of the properties above is present, the
 * bundle object identity will be used as context path.</li>
 * </ol>
 * 
 * @author Costin Leau
 * 
 * @see Bundle#getLocation()
 * @see Constants#BUNDLE_NAME
 * @see Bundle#getSymbolicName()
 * @see System#identityHashCode(Object)
 */
public class DefaultContextPathStrategy implements ContextPathStrategy {

	private static final String SLASH = "/";


	public String getContextPath(Bundle bundle) {
		Assert.notNull(bundle);

		String location = bundle.getLocation();
		String path = null;
		if (StringUtils.hasText(location)) {
			path = StringUtils.getFilename(StringUtils.cleanPath(location));
			// remove file extension
			path = StringUtils.stripFilenameExtension(path);
		}
		if (!StringUtils.hasText(path)) {
			// fall-back to bundle name
			path = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);

			if (!StringUtils.hasText(path)) {
				// fall-back to bundle sym name
				path = bundle.getSymbolicName();

				// fall-back to object identity
				if (!StringUtils.hasText(path)) {
					path = ClassUtils.getShortName(bundle.getClass()).concat(ObjectUtils.getIdentityHexString(bundle));
				}
			}
		}
		return (path.startsWith(SLASH) ? path : SLASH.concat(path));
	}
}
