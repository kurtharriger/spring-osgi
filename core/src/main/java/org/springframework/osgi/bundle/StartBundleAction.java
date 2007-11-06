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
package org.springframework.osgi.bundle;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.springframework.osgi.bundle.BundleTemplate.BundleCallback;

/**
 * {@link Bundle} start action. It will install the bundle if the given one is
 * null in order to start it.
 * 
 * @see Bundle#start()
 * 
 * @author Costin Leau
 * 
 */
public class StartBundleAction implements BundleAction {

	private static final BundleCallback startBundle = new BundleCallback() {
		public void execute(Bundle bundle) throws BundleException {
			bundle.start();
		}
	};

	public Bundle execute(Bundle bundle) {
		new BundleTemplate(bundle).executeCallback(startBundle);
		return bundle;
	}
}
