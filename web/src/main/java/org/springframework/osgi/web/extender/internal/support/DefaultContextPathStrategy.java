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

package org.springframework.osgi.web.extender.internal.support;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.springframework.osgi.web.extender.ContextPathStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link ContextPathStrategy} interface that
 * considers the OSGi bundle properties.
 * 
 * <p/> This implementation will use the bundle location filename, falling back
 * to the bundle name and, in case that's missing, to the bundle symbolic name.
 * If neither of these properties is available then the bundle object identity
 * will be used.
 * 
 * @author Costin Leau
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
