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
 * {@link Bundle} update action. It will install and start a bundle, if none is
 * available to execute the action upon.
 * 
 * @see Bundle#update()
 * 
 * @author Costin Leau
 */
public class UpdateBundleAction implements BundleAction {

	private final BundleCallback updateBundle = new BundleCallback() {

		public void execute(Bundle bundle) throws BundleException {
			bundle.update();
		}
	};

	public Bundle execute(Bundle bundle) {
		new BundleTemplate(bundle).executeCallback(updateBundle);
		return bundle;
	}

}