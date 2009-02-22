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

package org.springframework.osgi.blueprint.extender.internal.activator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.blueprint.context.ModuleContext;
import org.springframework.osgi.blueprint.context.SpringModuleContext;
import org.springframework.osgi.blueprint.context.support.ModuleContextServicePublisher;
import org.springframework.osgi.blueprint.extender.internal.activator.support.BlueprintConfigUtils;
import org.springframework.osgi.blueprint.extender.internal.event.EventAdminDispatcher;
import org.springframework.osgi.blueprint.extender.internal.support.BlueprintExtenderConfiguration;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.DelegatedExecutionOsgiBundleApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticaster;
import org.springframework.osgi.extender.internal.activator.ContextLoaderListener;
import org.springframework.osgi.extender.internal.support.ExtenderConfiguration;
import org.springframework.osgi.extender.support.internal.ConfigUtils;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * RFC124 extension to the Spring DM extender.
 * 
 * @author Costin Leau
 */
public class BlueprintModuleListener extends ContextLoaderListener {

	private BundleContext bundleContext;
	private EventAdminDispatcher dispatcher;
	private Version bluePrintExtenderVersion;
	private ModuleContextListenerManager listenerManager;


	@Override
	public void start(BundleContext context) throws Exception {
		this.bundleContext = context;
		this.dispatcher = new EventAdminDispatcher(bundleContext);
		this.bluePrintExtenderVersion = OsgiBundleUtils.getBundleVersion(context.getBundle());
		this.listenerManager = new ModuleContextListenerManager(context);
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		listenerManager.destroy();
		super.stop(context);
	}

	@Override
	protected boolean matchExtenderVersion(Bundle bundle, Version expectedExtenderVersion) {
		String bundleString = "[" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "]";

		if (!ConfigUtils.matchExtenderVersionRange(bundle, bluePrintExtenderVersion)) {
			if (log.isDebugEnabled())
				log.debug("Bundle " + bundleString + " expects an extender w/ version["
						+ OsgiBundleUtils.getHeaderAsVersion(bundle, BlueprintConfigUtils.EXTENDER_VERSION)
						+ "] which does not match current extender w/ version[" + bluePrintExtenderVersion
						+ "]; skipping bundle from context creation");
			return false;
		}

		return true;
	}

	protected ExtenderConfiguration initExtenderConfiguration(BundleContext bundleContext) {
		return new BlueprintExtenderConfiguration(bundleContext);
	}

	@Override
	protected void maybeAddNamespaceHandlerFor(Bundle bundle) {
		log.warn("Igorning namespace handling");
	}

	@Override
	protected void maybeRemoveNameSpaceHandlerFor(Bundle bundle) {
		log.warn("Igorning namespace handling");
	}

	@Override
	protected void preProcessClose(ConfigurableOsgiBundleApplicationContext context) {
		dispatcher.beforeClose(context);
		super.preProcessClose(context);
	}

	@Override
	protected void preProcessRefresh(ConfigurableOsgiBundleApplicationContext context) {
		BundleContext bundleContext = context.getBundleContext();
		// create the ModuleContext adapter
		ModuleContext mc = new SpringModuleContext(context, bundleContext);
		// add service publisher
		context.addApplicationListener(new ModuleContextServicePublisher(mc));

		dispatcher.beforeRefresh(context);
		super.preProcessRefresh(context);
	}

	@Override
	protected void postProcessClose(ConfigurableOsgiBundleApplicationContext context) {
		dispatcher.afterClose(context);
		super.postProcessClose(context);
	}

	@Override
	protected void postProcessRefresh(ConfigurableOsgiBundleApplicationContext context) {
		dispatcher.afterRefresh(context);

		Bundle bundle = context.getBundle();
		listenerManager.contextCreated(bundle);

		super.postProcessRefresh(context);
	}

	@Override
	protected void postProcessRefreshFailure(DelegatedExecutionOsgiBundleApplicationContext context, Throwable th) {
		dispatcher.refreshFailure(context, th);

		Bundle bundle = context.getBundle();
		listenerManager.contextCreationFailed(bundle, th);

		super.postProcessRefreshFailure(context, th);
	}

	@Override
	protected void addApplicationListener(OsgiBundleApplicationContextEventMulticaster multicaster) {
		super.addApplicationListener(multicaster);
		// monitor bootstrapping events
		multicaster.addApplicationListener(dispatcher);
	}
}