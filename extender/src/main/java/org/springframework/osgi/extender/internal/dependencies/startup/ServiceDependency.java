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

package org.springframework.osgi.extender.internal.dependencies.startup;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.springframework.osgi.service.importer.OsgiServiceDependency;
import org.springframework.osgi.service.importer.OsgiServiceImportDependencyDefinition;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;

/**
 * Holder/helper class representing an OSGi service dependency
 * 
 * @author Costin Leau
 * @author Hal Hildebrand
 * @author Andy Piper
 */
public class ServiceDependency implements OsgiServiceImportDependencyDefinition {

	protected final Filter filter;

	protected final String filterAsString;

	protected final boolean isMandatory;

	protected final BundleContext bundleContext;

	private final String beanName;

	private OsgiServiceDependency serviceDependency;


	public ServiceDependency(BundleContext bc, Filter serviceFilter, boolean isMandatory, String beanName) {
		filter = serviceFilter;
		this.filterAsString = filter.toString();
		this.isMandatory = isMandatory;
		bundleContext = bc;
		this.beanName = beanName;
		serviceDependency = new OsgiServiceDependency() {

			public String getBeanName() {
				return ServiceDependency.this.beanName;
			}

			public Filter getServiceFilter() {
				return ServiceDependency.this.filter;
			}

			public boolean isMandatoryService() {
				return ServiceDependency.this.isMandatory;
			}

		};
	}

	public boolean matches(ServiceEvent event) {
		return filter.match(event.getServiceReference());
	}

	/**
	 * @return
	 */
	public boolean isServicePresent() {
		return (!isMandatory || OsgiServiceReferenceUtils.isServicePresent(bundleContext, filterAsString));
	}

	public String toString() {
		return "Dependency on [" + filterAsString + "] (from bean [" + beanName + "])";
	}

	/**
	 * @return Returns the filter.
	 */
	public Filter getFilter() {
		return filter;
	}

	/**
	 * Returns the beanName.
	 * 
	 * @return Returns the beanName
	 */
	public String getBeanName() {
		return beanName;
	}

	public boolean isMandatory() {
		return isMandatory;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		final ServiceDependency that = (ServiceDependency) o;

		if (isMandatory != that.isMandatory)
			return false;
		if (filterAsString != null ? !filterAsString.equals(that.filterAsString) : that.filterAsString != null)
			return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (filterAsString != null ? filterAsString.hashCode() : 0);
		result = 29 * result + (isMandatory ? 1 : 0);
		return result;
	}

	public OsgiServiceDependency getServiceDependency() {
		return serviceDependency;
	}
}
