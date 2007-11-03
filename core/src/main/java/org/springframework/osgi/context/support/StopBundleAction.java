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
 * @author Costin Leau
 * 
 */
public class StopBundleAction implements BundleAction {

	private static final BundleCallback stopBundle = new BundleCallback() {
		public void execute(Bundle bundle) throws BundleException {
			bundle.stop();
		}
	};

	public Bundle execute(Bundle bundle) {
		if (bundle != null) {
			new BundleTemplate(bundle).executeCallback(stopBundle);
		}
		return bundle;
	}

}
