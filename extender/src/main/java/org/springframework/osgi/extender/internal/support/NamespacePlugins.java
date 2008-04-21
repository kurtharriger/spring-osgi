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

package org.springframework.osgi.extender.internal.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.core.CollectionFactory;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.ReflectionUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Spring schema handler/resolver for OSGi environments.
 * 
 * Besides delegation this class also does type filtering to avoid wiring the
 * wrong bundle if multiple versions of the same library (which support the same
 * schema) are available.
 * 
 * @author Hal Hildebrand
 * @author Costin Leau
 * 
 */
public class NamespacePlugins implements NamespaceHandlerResolver, EntityResolver, DisposableBean {

	/**
	 * Wrapper class which implements both {@link EntityResolver} and
	 * {@link NamespaceHandlerResolver} interfaces.
	 * 
	 * Simply delegates to the actual implementation discovered in a specific
	 * bundle.
	 */
	private static class Plugin implements NamespaceHandlerResolver, EntityResolver {

		private final NamespaceHandlerResolver namespace;

		private final EntityResolver entity;

		private final Bundle bundle;


		private Plugin(Bundle bundle) {
			this.bundle = bundle;

			ClassLoader loader = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle);

			entity = new DelegatingEntityResolver(loader);
			namespace = new DefaultNamespaceHandlerResolver(loader);
		}

		public NamespaceHandler resolve(String namespaceUri) {
			return namespace.resolve(namespaceUri);
		}

		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			return entity.resolveEntity(publicId, systemId);
		}

		public Bundle getBundle() {
			return bundle;
		}
	}


	private static final Log log = LogFactory.getLog(NamespacePlugins.class);

	private static final String CACHE_CLASS = "org.springframework.osgi.context.support.TrackingUtil";

	private static final String FIELD_NAME = "invokingBundle";

	private final Map plugins = CollectionFactory.createConcurrentMap(5);

	/** hold a direct reference since it's a mandatory platform service */
	private final PackageAdmin pa;


	NamespacePlugins(PackageAdmin packageAdmin) {
		this.pa = packageAdmin;
	}

	public void addHandler(Bundle bundle) {
		if (log.isDebugEnabled())
			log.debug("Adding as handler " + OsgiStringUtils.nullSafeNameAndSymName(bundle));

		plugins.put(bundle, new Plugin(bundle));
	}

	/**
	 * Return true if a handler mapping was removed for the given bundle.
	 * 
	 * @param bundle bundle to look at
	 * @return true if the bundle was used in the plugin map
	 */
	public boolean removeHandler(Bundle bundle) {
		if (log.isDebugEnabled())
			log.debug("Removing handler " + OsgiStringUtils.nullSafeNameAndSymName(bundle));

		return (plugins.remove(bundle) != null);
	}

	public NamespaceHandler resolve(String namespaceUri) {
		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("Trying to resolving namespace handler for " + namespaceUri);

		// avoid creation if there is no package admin
		Map possibleProviders = (pa == null ? null : new LinkedHashMap(4));

		for (Iterator i = plugins.values().iterator(); i.hasNext();) {
			Plugin plugin = (Plugin) i.next();
			try {
				NamespaceHandler handler = plugin.resolve(namespaceUri);
				if (handler != null) {
					if (trace)
						log.trace("Namespace handler for " + namespaceUri + " found inside "
								+ OsgiStringUtils.nullSafeNameAndSymName(plugin.getBundle()));

					// no package admin, just bail out
					if (pa == null)
						return handler;

					// add bundle to the map
					possibleProviders.put(plugin.getBundle(), handler);
				}
			}
			catch (IllegalArgumentException ex) {
				if (trace)
					log.trace("Namespace handler for " + namespaceUri + " not found inside "
							+ OsgiStringUtils.nullSafeNameAndSymName(plugin.getBundle()));

			}
		}

		// no provider found
		if (pa == null || possibleProviders.isEmpty())
			return null;

		// filter provider
		Bundle provider = filterProvider(possibleProviders.keySet());
		return (provider == null ? null : (NamespaceHandler) possibleProviders.get(provider));
	}

	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("Trying to resolving entity for " + publicId + "|" + systemId);

		// avoid creation if there is no package admin
		Map possibleProviders = (pa == null ? null : new LinkedHashMap(4));

		if (systemId != null) {
			for (Iterator i = plugins.values().iterator(); i.hasNext();) {
				InputSource inputSource;
				Plugin plugin = (Plugin) i.next();
				try {
					inputSource = plugin.resolveEntity(publicId, systemId);
					if (inputSource != null) {
						if (trace)
							log.trace("XML schema for " + publicId + "|" + systemId + " found inside "
									+ OsgiStringUtils.nullSafeNameAndSymName(plugin.getBundle()));

						// no package admin, just bail out
						if (pa == null)
							return inputSource;

						// add bundle to the map
						possibleProviders.put(plugin.getBundle(), inputSource);
					}

				}
				catch (FileNotFoundException ex) {
					if (trace)
						log.trace("XML schema for " + publicId + "|" + systemId + " not found inside "
								+ OsgiStringUtils.nullSafeNameAndSymName(plugin.getBundle()), ex);
				}
			}
		}
		// no provider found
		if (pa == null || possibleProviders.isEmpty())
			return null;

		// filter provider
		Bundle provider = filterProvider(possibleProviders.keySet());
		return (provider == null ? null : (InputSource) possibleProviders.get(provider));
	}

	public void destroy() {
		plugins.clear();
	}

	/**
	 * Filter the possible provides based on the wiring to the requesting
	 * bundle.
	 * 
	 * @param possibleProviders
	 * @return
	 */
	private Bundle filterProvider(Collection possibleProviders) {
		boolean trace = log.isTraceEnabled();
		Bundle invokingBundle = getInvokingBundle();

		// cannot find the invoking bundle, return the first provider found
		if (invokingBundle == null) {
			if (trace)
				log.trace("No invoking bundle found; returning the first namespace/resolver found");
		}

		// use the package admin to find the exported packages of the given providers
		for (Iterator iterator = possibleProviders.iterator(); iterator.hasNext();) {
			Bundle possibleProvider = (Bundle) iterator.next();
			// check the exported packages
			ExportedPackage[] packages = pa.getExportedPackages(possibleProvider);
			for (int packagesIndex = 0; packages != null && packagesIndex < packages.length; packagesIndex++) {
				ExportedPackage exportedPackage = packages[packagesIndex];
				// to discover the importing bundles
				Bundle[] importingBundles = exportedPackage.getImportingBundles();

				for (int importersIndex = 0; importingBundles != null && importersIndex < importingBundles.length; importersIndex++) {
					Bundle importer = importingBundles[importersIndex];
					// if the invoking bundle is wired to the provider, we have a match
					if (importer.equals(invokingBundle)) {
						if (trace)
							log.trace("Found wiring between invoker "
									+ OsgiStringUtils.nullSafeSymbolicName(invokingBundle) + " and namespace provider "
									+ OsgiStringUtils.nullSafeSymbolicName(possibleProvider));
						return possibleProvider;
					}
				}
			}
		}
		// there is no wiring between the providers and the target so any handler will do
		// return the first one found
		Bundle match = (Bundle) possibleProviders.iterator().next();
		if (trace)
			log.trace("No wiring between the invoker bundle " + OsgiStringUtils.nullSafeSymbolicName(invokingBundle)
					+ " and the handlers found; returning the first namespace provider "
					+ OsgiStringUtils.nullSafeSymbolicName(match));
		return match;
	}

	/**
	 * Returns the namespace/resolver invoker plugin. To do that, the Spring-DM
	 * core classes will be used assuming that its infrastructure is being used.
	 * 
	 * @return the invoking bundle
	 */
	private Bundle getInvokingBundle() {
		// get the spring-dm core class loader
		ClassLoader coreClassLoader = OsgiStringUtils.class.getClassLoader();
		try {
			Class cacheClass = coreClassLoader.loadClass(CACHE_CLASS);
			Field field = cacheClass.getField(FIELD_NAME);
			ReflectionUtils.makeAccessible(field);
			return (Bundle) ((ThreadLocal) field.get(null)).get();
		}
		catch (Exception ex) {
			log.trace("Could not determine invoking bundle", ex);
			return null;
		}
	}
}
