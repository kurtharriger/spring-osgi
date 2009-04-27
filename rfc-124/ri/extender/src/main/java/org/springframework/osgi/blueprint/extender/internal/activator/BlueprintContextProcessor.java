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

package org.springframework.osgi.blueprint.extender.internal.activator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.context.ModuleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.osgi.blueprint.context.SpringModuleContext;
import org.springframework.osgi.blueprint.context.support.ModuleContextServicePublisher;
import org.springframework.osgi.blueprint.convert.SpringConversionService;
import org.springframework.osgi.blueprint.extender.internal.event.EventAdminDispatcher;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.extender.internal.activator.OsgiContextProcessor;

/**
 * Blueprint specific context processor.
 * 
 * @author Costin Leau
 */
public class BlueprintContextProcessor implements OsgiContextProcessor {

	private final EventAdminDispatcher dispatcher;
	private final ModuleContextListenerManager listenerManager;


	public BlueprintContextProcessor(EventAdminDispatcher dispatcher, ModuleContextListenerManager listenerManager) {
		this.dispatcher = dispatcher;
		this.listenerManager = listenerManager;
	}

	public void postProcessClose(ConfigurableOsgiBundleApplicationContext context) {
		dispatcher.afterClose(context);
	}

	public void postProcessRefresh(ConfigurableOsgiBundleApplicationContext context) {
		dispatcher.afterRefresh(context);

		Bundle bundle = context.getBundle();
		listenerManager.contextCreated(bundle);

	}

	public void postProcessRefreshFailure(ConfigurableOsgiBundleApplicationContext context, Throwable th) {
		dispatcher.refreshFailure(context, th);

		Bundle bundle = context.getBundle();
		listenerManager.contextCreationFailed(bundle, th);
	}

	public void preProcessClose(ConfigurableOsgiBundleApplicationContext context) {
		dispatcher.beforeClose(context);
	}

	public void preProcessRefresh(final ConfigurableOsgiBundleApplicationContext context) {
		BundleContext bundleContext = context.getBundleContext();
		// create the ModuleContext adapter
		final ModuleContext mc = new SpringModuleContext(context, bundleContext);
		// add service publisher
		context.addApplicationListener(new ModuleContextServicePublisher(mc));
		// add moduleContext bean
		context.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {

			private static final String MODULE_CONTEXT_BEAN_NAME = "moduleContext";
			private static final String CONVERSION_SERVICE_BEAN_NAME = "conversionService";


			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
				// lazy logger evaluation
				Log logger = LogFactory.getLog(context.getClass());

				// add module context bean
				addPredefinedBean(beanFactory, MODULE_CONTEXT_BEAN_NAME, mc, logger);
				addPredefinedBean(beanFactory, CONVERSION_SERVICE_BEAN_NAME, new SpringConversionService(beanFactory),
					logger);
			}

			private void addPredefinedBean(ConfigurableListableBeanFactory beanFactory, String beanName, Object value,
					Log logger) {
				if (!beanFactory.containsLocalBean(beanName)) {
					logger.debug("Registering pre-defined bean named " + beanName);
					beanFactory.registerSingleton(beanName, value);
				}
				else {
					logger.warn("A bean named " + beanName
							+ " already exists; aborting registration of the predefined value...");
				}
			}
		});

		dispatcher.beforeRefresh(context);
	}
}