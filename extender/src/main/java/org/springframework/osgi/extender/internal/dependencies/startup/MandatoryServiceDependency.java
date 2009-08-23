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

package org.springframework.osgi.extender.internal.dependencies.startup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.springframework.osgi.service.importer.DefaultOsgiServiceDependency;
import org.springframework.osgi.service.importer.OsgiServiceDependency;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.util.ObjectUtils;

/**
 * Holder/helper class representing an OSGi service dependency
 * 
 * @author Costin Leau
 * @author Hal Hildebrand
 * @author Andy Piper
 */
class MandatoryServiceDependency implements OsgiServiceDependency {
	// match the class inside object class (and use a non backing reference group)
	private static final Pattern PATTERN = Pattern.compile("objectClass=(?:[^\\)]+)");

	protected final BundleContext bundleContext;

	private OsgiServiceDependency serviceDependency;
	private final AtomicInteger matchingServices = new AtomicInteger(0);
	protected final String filterAsString;
	private final String[] classes;

	MandatoryServiceDependency(BundleContext bc, Filter serviceFilter, boolean isMandatory, String beanName) {
		this(bc, new DefaultOsgiServiceDependency(beanName, serviceFilter, isMandatory));
	}

	MandatoryServiceDependency(BundleContext bc, OsgiServiceDependency dependency) {
		bundleContext = bc;
		serviceDependency = dependency;
		this.filterAsString = dependency.getServiceFilter().toString();
		this.classes = extractObjectClassFromFilter(filterAsString);
	}

	boolean matches(ServiceEvent event) {
		return serviceDependency.getServiceFilter().match(event.getServiceReference());
	}

	boolean isServicePresent() {
		if (serviceDependency.isMandatory()) {
			// check the service presence but use the classes (if discovered) to
			// trigger security checks (if the sm is enabled)
			if (!ObjectUtils.isEmpty(classes) && System.getSecurityManager() != null) {
				try {
					for (String className : classes) {
						if (ObjectUtils.isEmpty(bundleContext.getServiceReferences(className, filterAsString))) {
							return false;
						}
					}
					return true;
				} catch (InvalidSyntaxException ise) {
				}
			} else {
				return OsgiServiceReferenceUtils.isServicePresent(bundleContext, filterAsString);
			}
		}
		return true;
	}

	public String toString() {
		return "Dependency on [" + filterAsString + "] (from bean [" + serviceDependency.getBeanName() + "])";
	}

	public Filter getServiceFilter() {
		return serviceDependency.getServiceFilter();
	}

	public String getBeanName() {
		return serviceDependency.getBeanName();
	}

	public boolean isMandatory() {
		return serviceDependency.isMandatory();
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		final MandatoryServiceDependency that = (MandatoryServiceDependency) o;

		return (serviceDependency.equals(that.serviceDependency));
	}

	public int hashCode() {
		int result = MandatoryServiceDependency.class.hashCode();
		result = 29 * result + serviceDependency.hashCode();
		return result;
	}

	public OsgiServiceDependency getServiceDependency() {
		return serviceDependency;
	}

	/**
	 * Adds another matching service.
	 * 
	 * @return the counter after adding the service.
	 */
	int increment() {
		return matchingServices.incrementAndGet();
	}

	/**
	 * Removes a matching service.
	 * 
	 * @return the counter after substracting the service.
	 */
	int decrement() {
		return matchingServices.decrementAndGet();
	}

	private static String[] extractObjectClassFromFilter(String filterString) {
		List<String> matches = null;
		Matcher matcher = PATTERN.matcher(filterString);
		while (matcher.find()) {
			if (matches == null) {
				matches = new ArrayList<String>(4);
			}

			matches.add(matcher.group());
		}

		return (matches == null ? new String[0] : matches.toArray(new String[matches.size()]));
	}
}