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

package org.springframework.osgi.samples.console.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.samples.console.service.BundleDisplayOption;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Simple POJO used for passing information to the view.
 * 
 * @author Costin Leau
 */
public class BundleInfo {

	public static class OsgiService {

		private final Collection<String> usingBundles;
		private final Map<String, Object> properties;
		private final String bundle;


		public OsgiService(ServiceReference reference, BundleDisplayOption displayOption) {
			Hashtable<String, Object> props = new Hashtable<String, Object>();
			for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>) OsgiServiceReferenceUtils.getServicePropertiesSnapshotAsMap(
				reference).entrySet()) {
				props.put(entry.getKey(), ObjectUtils.nullSafeToString(entry.getValue()));
			}
			properties = Collections.unmodifiableMap(props);

			bundle = displayOption.display(reference.getBundle());
			Collection<String> usingBundlesString = new ArrayList<String>();
			Bundle[] usingBndls = reference.getUsingBundles();
			if (usingBndls != null)
				for (Bundle usingBundle : usingBndls) {
					usingBundlesString.add(displayOption.display(usingBundle));
				}
			usingBundles = Collections.unmodifiableCollection(usingBundlesString);
		}

		public Collection<String> getUsingBundles() {
			return usingBundles;
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public String getBundle() {
			return bundle;
		}
	}


	private final Map<String, Object> properties = new LinkedHashMap<String, Object>();
	private final String state;
	private final Date lastModified;
	private final Collection<String> exportedPackages = new ArrayList<String>();
	private final Collection<String> importedPackages = new ArrayList<String>();
	private final Collection<OsgiService> registeredServices = new ArrayList<OsgiService>();
	private final Collection<OsgiService> servicesInUse = new ArrayList<OsgiService>();
	private final Bundle bundle;


	public BundleInfo(Bundle bundle) {
		this.bundle = bundle;
		// initialize properties
		Dictionary headers = bundle.getHeaders();
		addKeyValueForHeader(Constants.BUNDLE_ACTIVATOR, headers);
		addKeyValueForHeader(Constants.BUNDLE_CLASSPATH, headers);
		addKeyValueForHeader(Constants.BUNDLE_NAME, headers);
		addKeyValueForHeader(Constants.BUNDLE_SYMBOLICNAME, headers);
		properties.put(Constants.BUNDLE_VERSION, OsgiBundleUtils.getBundleVersion(bundle));
		this.state = OsgiStringUtils.bundleStateAsString(bundle);
		this.lastModified = new Date(bundle.getLastModified());

	}

	private void addKeyValueForHeader(String headerName, Dictionary headers) {
		properties.put(headerName, headers.get(headerName));
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void addProperty(String name, Object value) {
		properties.put(name, (value == null ? "" : value));
	}

	public Bundle getBundle() {
		return bundle;
	}

	public String getLocation() {
		return bundle.getLocation();
	}

	public String getState() {
		return state;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public Collection<String> getExportedPackages() {
		return exportedPackages;
	}

	public void addExportedPackages(String... exportedPackage) {
		CollectionUtils.mergeArrayIntoCollection(exportedPackage, exportedPackages);
	}

	public Collection<String> getImportedPackages() {
		return importedPackages;
	}

	public void addImportedPackages(String... importedPackage) {
		CollectionUtils.mergeArrayIntoCollection(importedPackage, importedPackages);
	}

	public Collection<OsgiService> getRegisteredServices() {
		return registeredServices;
	}

	public void addRegisteredServices(OsgiService registeredService) {
		this.registeredServices.add(registeredService);
	}

	public Collection<OsgiService> getServicesInUse() {
		return servicesInUse;
	}

	public void addServiceInUse(OsgiService serviceInUse) {
		this.servicesInUse.add(serviceInUse);
	}

}
