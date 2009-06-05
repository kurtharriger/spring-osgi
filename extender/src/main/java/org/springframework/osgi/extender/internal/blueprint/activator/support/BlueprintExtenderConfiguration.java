/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.extender.internal.blueprint.activator.support;

import org.apache.commons.logging.Log;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.extender.OsgiApplicationContextCreator;
import org.springframework.osgi.extender.internal.support.ExtenderConfiguration;

/**
 * Extension of the default extender configuration for handling RFC 124 extender semantics.
 * 
 * @author Costin Leau
 * 
 */
public class BlueprintExtenderConfiguration extends ExtenderConfiguration {

	private final OsgiApplicationContextCreator contextCreator = new BlueprintContainerCreator();

	/**
	 * Constructs a new <code>BlueprintExtenderConfiguration</code> instance.
	 * 
	 * @param bundleContext
	 */
	public BlueprintExtenderConfiguration(BundleContext bundleContext, Log log) {
		super(bundleContext, log);
	}

	public OsgiApplicationContextCreator getContextCreator() {
		return contextCreator;
	}
}
