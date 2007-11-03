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
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.Assert;

/**
 * Simple wrapper for dealing with BundleExceptions.
 * 
 * @author Costin Leau
 * 
 */
// TODO: consider moving this class into an util package
class BundleTemplate {

	private final Bundle bundle;

	//private final BundleContext bundleContext;

	BundleTemplate(Bundle bundle) {
		Assert.notNull(bundle);
		this.bundle = bundle;
	}

	BundleTemplate(BundleContext bundleContext) {
		Assert.notNull(bundleContext);
		//this.bundleContext = bundleContext;
		this.bundle = bundleContext.getBundle();
	}

	interface BundleCallback {
		void execute(Bundle bundle) throws BundleException;
	}

	interface BundleContextCallback {
		void execute(BundleContext bundleContext) throws BundleException;
	}

	void executeCallback(BundleCallback callback) {
		Assert.notNull(callback);
		try {
			callback.execute(bundle);
		}
		catch (BundleException ex) {
			throw (RuntimeException) new IllegalStateException("exception occured while working with bundle "
					+ OsgiStringUtils.nullSafeNameAndSymName(bundle)).initCause(ex);
		}
	}

//	void executeCallback(BundleContextCallback callback) {
//		Assert.notNull(callback);
//		try {
//			callback.execute(bundleContext);
//		}
//		catch (BundleException ex) {
//			throw (RuntimeException) new IllegalStateException("exception occured while working with bundleContext "
//					+ bundleContext).initCause(ex);
//		}
//	}
}
