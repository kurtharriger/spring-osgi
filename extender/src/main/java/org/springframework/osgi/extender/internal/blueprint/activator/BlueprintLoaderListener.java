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

package org.springframework.osgi.extender.internal.blueprint.activator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticaster;
import org.springframework.osgi.extender.internal.activator.ApplicationContextConfigurationFactory;
import org.springframework.osgi.extender.internal.activator.ContextLoaderListener;
import org.springframework.osgi.extender.internal.activator.OsgiContextProcessor;
import org.springframework.osgi.extender.internal.blueprint.activator.support.BlueprintConfigUtils;
import org.springframework.osgi.extender.internal.blueprint.activator.support.BlueprintContainerConfig;
import org.springframework.osgi.extender.internal.blueprint.activator.support.BlueprintExtenderConfiguration;
import org.springframework.osgi.extender.internal.blueprint.event.EventAdminDispatcher;
import org.springframework.osgi.extender.internal.support.ExtenderConfiguration;
import org.springframework.osgi.extender.support.ApplicationContextConfiguration;

/**
 * RFC124 extension to the Spring DM extender.
 * 
 * @author Costin Leau
 */
public class BlueprintLoaderListener extends ContextLoaderListener {

	private volatile EventAdminDispatcher dispatcher;
	private volatile BlueprintListenerManager listenerManager;
	private volatile Bundle bundle;
	private volatile BlueprintContainerProcessor contextProcessor;

	@Override
	public void start(BundleContext context) throws Exception {
		this.listenerManager = new BlueprintListenerManager(context);
		this.dispatcher = new EventAdminDispatcher(context);
		this.bundle = context.getBundle();
		this.contextProcessor = new BlueprintContainerProcessor(dispatcher, listenerManager, bundle);

		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		listenerManager.destroy();
		super.stop(context);
	}

	@Override
	protected ExtenderConfiguration initExtenderConfiguration(BundleContext bundleContext) {
		return new BlueprintExtenderConfiguration(bundleContext, log);
	}

	@Override
	protected ApplicationContextConfigurationFactory createContextConfigFactory() {
		return new ApplicationContextConfigurationFactory() {

			public ApplicationContextConfiguration createConfiguration(Bundle bundle) {
				return new BlueprintContainerConfig(bundle);
			}
		};
	}

	@Override
	protected OsgiContextProcessor createContextProcessor() {
		return contextProcessor;
	}

	@Override
	protected String getManagedBundleExtenderVersionHeader() {
		return BlueprintConfigUtils.EXTENDER_VERSION;
	}

	@Override
	protected void addApplicationListener(OsgiBundleApplicationContextEventMulticaster multicaster) {
		super.addApplicationListener(multicaster);
		// monitor bootstrapping events
		multicaster.addApplicationListener(contextProcessor);
	}

	protected ApplicationContextConfiguration createContextConfig(Bundle bundle) {
		return new BlueprintContainerConfig(bundle);
	}
}