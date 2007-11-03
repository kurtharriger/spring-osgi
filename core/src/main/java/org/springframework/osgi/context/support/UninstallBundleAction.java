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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.springframework.osgi.context.support.BundleTemplate.BundleCallback;

/**
 * Uninstall {@link Bundle} action. If an invalid (null) bundle is given, no
 * action will be executed (i.e. no bundle means nothing to uninstall).
 * 
 * @author Costin Leau
 * 
 */
public class UninstallBundleAction implements BundleAction {

	private static final BundleCallback uninstallBundle = new BundleCallback() {
		public void execute(Bundle bundle) throws BundleException {
			bundle.uninstall();
		}
	};

	public Bundle execute(Bundle bundle) {
		if (bundle != null) {
			new BundleTemplate(bundle).executeCallback(uninstallBundle);
		}

		return bundle;
	}

}
