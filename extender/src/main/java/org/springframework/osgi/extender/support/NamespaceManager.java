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
package org.springframework.osgi.extender.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.springframework.osgi.context.support.NamespacePlugins;
import org.springframework.osgi.context.support.OsgiBundleNamespaceHandlerAndEntityResolver;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiPlatformDetector;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.Assert;

/**
 * Support class that deals with namespace parsers discovered inside Spring
 * bundles.
 * 
 * @author Costin Leau
 * 
 */
public class NamespaceManager implements InitializingBean, DisposableBean {

	private static final Log log = LogFactory.getLog(NamespaceManager.class);

	/**
	 * Are we running under knoplerfish? Required for bug workaround with
	 * calling getResource under KF (bug #1581187 confirmed and tested in our
	 * test suite).
	 */
	private final boolean isKnopflerfish;

	/**
	 * The set of all namespace plugins known to the extender
	 */
	private NamespacePlugins namespacePlugins;

	/**
	 * ServiceRegistration object returned by OSGi when registering the
	 * NamespacePlugins instance as a service
	 */
	private ServiceRegistration resolverServiceRegistration = null;

	/**
	 * OSGi Environment.
	 */
	private final BundleContext context;

	private static final String SPRING_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";

	private static final String[] OSGI_BUNDLE_RESOLVER_INTERFACE_NAME = { OsgiBundleNamespaceHandlerAndEntityResolver.class.getName() };

	/**
	 * Constructor.
	 * 
	 * @param extenderBundleContext
	 */
	public NamespaceManager(BundleContext context) {
		this.context = context;
		this.isKnopflerfish = OsgiPlatformDetector.isKnopflerfish(context);
		this.namespacePlugins = new NamespacePlugins();
	}

	/**
	 * If this bundle defines handler mapping or schema mapping resources, then
	 * register it with the namespace plugin handler.
	 * 
	 * @param bundle
	 */
	// TODO: what about custom locations (outside of META-INF/spring)
	// FIXME: rely on OSGI-IO here
	public void maybeAddNamespaceHandlerFor(Bundle bundle) {
		// Ignore system bundle
		if (OsgiBundleUtils.isSystemBundle(bundle)) {
			return;
		}
		if (isKnopflerfish) {
			// KF (2.0.0-2.0.1) throws a ClassCastException
			// http://sourceforge.net/tracker/index.php?func=detail&aid=1581187&group_id=82798&atid=567241
			if (bundle.getEntry(SPRING_HANDLER_MAPPINGS_LOCATION) != null
					|| bundle.getEntry(PluggableSchemaResolver.DEFAULT_SCHEMA_MAPPINGS_LOCATION) != null) {
				addHandler(bundle);
			}
		}
		else {
			if (bundle.getResource(SPRING_HANDLER_MAPPINGS_LOCATION) != null
					|| bundle.getResource(PluggableSchemaResolver.DEFAULT_SCHEMA_MAPPINGS_LOCATION) != null) {
				addHandler(bundle);
			}
		}
	}

	/**
	 * Add this bundle to those known to provide handler or schema mappings.
	 * This method expects that the validity check (whatever that is) has been
	 * already done.
	 * 
	 * @param bundle
	 */
	protected void addHandler(Bundle bundle) {
		Assert.notNull(bundle);
		if (log.isDebugEnabled()) {
			log.debug("Adding namespace handler resolver for " + OsgiStringUtils.nullSafeNameAndSymName(bundle));
		}

		this.namespacePlugins.addHandler(bundle);
	}

	/**
	 * Remove this bundle from the set of those known to provide handler or
	 * schema mappings.
	 * 
	 * @param bundle
	 */
	public void maybeRemoveNameSpaceHandlerFor(Bundle bundle) {
		Assert.notNull(bundle);
		boolean removed = this.namespacePlugins.removeHandler(bundle);
		if (removed && log.isDebugEnabled()) {
			log.debug("Removed namespace handler resolver for " + OsgiStringUtils.nullSafeNameAndSymName(bundle));
		}
	}

	/**
	 * Register the NamespacePlugins instance as an Osgi Resolver service
	 */
	private ServiceRegistration registerResolverService() {
		if (log.isDebugEnabled()) {
			log.debug("Registering Spring NamespaceHandler and EntityResolver service");
		}

		return context.registerService(OSGI_BUNDLE_RESOLVER_INTERFACE_NAME, this.namespacePlugins, null);
	}

	/**
	 * Unregister the NamespaceHandler and EntityResolver service
	 */
	private void unregisterResolverService() {

		if (OsgiServiceUtils.unregisterService(resolverServiceRegistration)) {
			if (log.isDebugEnabled())
				log.debug("Unregistering Spring NamespaceHandler and EntityResolver service");
		}

		this.resolverServiceRegistration = null;
	}

	/**
	 * @return Returns the namespacePlugins.
	 */
	public NamespacePlugins getNamespacePlugins() {
		return namespacePlugins;
	}

	//
	// Lifecycle methods
	//

	public void afterPropertiesSet() {
		resolverServiceRegistration = registerResolverService();
	}

	public void destroy() {
		unregisterResolverService();
		this.namespacePlugins.destroy();
		this.namespacePlugins = null;
	}

}
