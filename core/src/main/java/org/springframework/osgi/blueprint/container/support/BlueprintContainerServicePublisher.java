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

package org.springframework.osgi.blueprint.container.support;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.util.ObjectUtils;

/**
 * Infrastructure bean that automatically publishes the given ModuleContext as an OSGi service. The bean listens for the
 * start/stop events inside an {@link ApplicationContext} to register/unregister the equivalent service.
 * 
 * <b>Note:</b> This component is stateful and should not be shared by multiple threads.
 * 
 * @author Costin Leau
 * 
 */
public class BlueprintContainerServicePublisher implements ApplicationListener<ApplicationContextEvent> {

	/** logger */
	private static final Log log = LogFactory.getLog(BlueprintContainerServicePublisher.class);

	private static final String BLUEPRINT_SYMNAME = "osgi.blueprint.container.symbolicname";
	private static final String BLUEPRINT_VERSION = "osgi.blueprint.container.version";

	private final BlueprintContainer blueprintContainer;
	private final BundleContext bundleContext;
	/** registration */
	private volatile ServiceRegistration registration;

	/**
	 * Constructs a new <code>ModuleContextServicePublisher</code> instance.
	 * 
	 * @param blueprintContainer
	 * @param bundleContext
	 */
	public BlueprintContainerServicePublisher(BlueprintContainer blueprintContainer) {
		this.blueprintContainer = blueprintContainer;
		this.bundleContext = blueprintContainer.getBundleContext();
	}

	public void onApplicationEvent(ApplicationContextEvent event) {
		// publish
		if (event instanceof ContextRefreshedEvent) {
			registerService();
		} else if (event instanceof ContextClosedEvent) {
			unregisterService();
		}
	}

	private void registerService() {
		Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();

		Bundle bundle = bundleContext.getBundle();
		// add RFC124 properties

		String symName = bundle.getSymbolicName();
		serviceProperties.put(Constants.BUNDLE_SYMBOLICNAME, symName);
		serviceProperties.put(BLUEPRINT_SYMNAME, symName);

		Version version = OsgiBundleUtils.getBundleVersion(bundle);
		serviceProperties.put(Constants.BUNDLE_VERSION, version);
		serviceProperties.put(BLUEPRINT_VERSION, version);

		log.info("Publishing ModuleContext as OSGi service with properties " + serviceProperties);

		// export just the interface
		String[] serviceNames = new String[] { BlueprintContainer.class.getName() };

		if (log.isDebugEnabled())
			log.debug("Publishing service under classes " + ObjectUtils.nullSafeToString(serviceNames));

		// publish service
		registration = bundleContext.registerService(serviceNames, blueprintContainer, serviceProperties);
	}

	private void unregisterService() {
		OsgiServiceUtils.unregisterService(registration);
		registration = null;
	}
}