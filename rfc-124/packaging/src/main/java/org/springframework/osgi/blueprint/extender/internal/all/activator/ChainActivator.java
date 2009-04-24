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

package org.springframework.osgi.blueprint.extender.internal.all.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.blueprint.extender.internal.activator.BlueprintModuleListener;
import org.springframework.osgi.extender.internal.activator.ContextLoaderListener;

/**
 * Bundle activator that simply the lifecycle callbacks to other activators.
 * 
 * @author Costin Leau
 * 
 */
public class ChainActivator implements BundleActivator {

	private final BundleActivator[] CHAIN = new BundleActivator[] { new ContextLoaderListener(),
		new BlueprintModuleListener() };


	public void start(BundleContext context) throws Exception {
		for (int i = 0; i < CHAIN.length; i++) {
			CHAIN[i].start(context);
		}
	}

	public void stop(BundleContext context) throws Exception {
		for (int i = CHAIN.length - 1; i >= 0; i--) {
			CHAIN[i].stop(context);
		}
	}
}
