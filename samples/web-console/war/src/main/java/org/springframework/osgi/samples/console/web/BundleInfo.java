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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;

/**
 * Simple POJO used for passing information to the view.
 * 
 * @author Costin Leau
 */
public class BundleInfo {

	static class OsgiService {

		private Collection<String> usingBundles = new ArrayList<String>();
		private Map<String, Object> properties = new LinkedHashMap<String, Object>();
		private String bundle;


		public Collection<String> getUsingBundles() {
			return usingBundles;
		}

		public void addUsingBundles(String usingBundle) {
			this.usingBundles.add(usingBundle);
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public void addProperty(String key, Object value) {
			this.properties.put(key, value);
		}

		public String getBundle() {
			return bundle;
		}

		public void setBundle(String bundle) {
			this.bundle = bundle;
		}
	}


	private Map<String, Object> properties = new LinkedHashMap<String, Object>();
	private String location;
	private String state;
	private Date lastModified;
	private Collection<String> exportedPackages = new ArrayList<String>();
	private Collection<String> importedPackages = new ArrayList<String>();
	private Collection<OsgiService> registeredServices = new ArrayList<OsgiService>();
	private Collection<OsgiService> servicesInUse = new ArrayList<OsgiService>();


	public Map<String, Object> getProperties() {
		return properties;
	}

	public void addProperty(String name, Object value) {
		properties.put(name, (value == null ? "" : value));
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
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
