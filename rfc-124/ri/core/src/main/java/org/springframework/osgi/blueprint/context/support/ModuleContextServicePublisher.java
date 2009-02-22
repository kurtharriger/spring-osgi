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

package org.springframework.osgi.blueprint.context.support;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.ModuleContextEventConstants;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.util.ObjectUtils;

/**
 * Infrastructure bean that automatically publishes the given ModuleContext as
 * an OSGi service. The bean listens for the start/stop events inside an
 * {@link ApplicationContext} to register/unregister the equivalent service.
 * 
 * <b>Note:</b> This component is stateful and should not be shared by multiple
 * threads.
 * 
 * @author Costin Leau
 * 
 */
public class ModuleContextServicePublisher implements ApplicationListener {

	/** logger */
	private static final Log log = LogFactory.getLog(ModuleContextServicePublisher.class);

	private final ModuleContext moduleContext;
	private final BundleContext bundleContext;
	/** registration */
	private volatile ServiceRegistration registration;


	/**
	 * Constructs a new <code>ModuleContextServicePublisher</code> instance.
	 * 
	 * @param moduleContext
	 * @param bundleContext
	 */
	public ModuleContextServicePublisher(ModuleContext moduleContext) {
		this.moduleContext = moduleContext;
		this.bundleContext = moduleContext.getBundleContext();
	}

	public void onApplicationEvent(ApplicationEvent event) {
		// publish 
		if (event instanceof ContextRefreshedEvent) {
			registerService();
		}
		else if (event instanceof ContextClosedEvent) {
			unregisterService();
		}
	}

	private void registerService() {
		Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();

		Bundle bundle = bundleContext.getBundle();
		// add RFC124 properties

		serviceProperties.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
		// FIXME: replace with ModuleContextEventConstants
		serviceProperties.put("bundle.symbolicName", bundle.getSymbolicName());

		Version version = OsgiBundleUtils.getBundleVersion(bundle);
		serviceProperties.put(Constants.BUNDLE_VERSION, version);
		serviceProperties.put(ModuleContextEventConstants.BUNDLE_VERSION, version);

		log.info("Publishing ModuleContext as OSGi service with properties " + serviceProperties);

		// export just the interface
		String[] serviceNames = new String[] { ModuleContext.class.getName() };

		if (log.isDebugEnabled())
			log.debug("Publishing service under classes " + ObjectUtils.nullSafeToString(serviceNames));

		// publish service
		registration = bundleContext.registerService(serviceNames, moduleContext, serviceProperties);
	}

	private void unregisterService() {
		OsgiServiceUtils.unregisterService(registration);
		registration = null;
	}
}