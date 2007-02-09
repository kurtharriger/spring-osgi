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
package org.springframework.osgi.service;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Utility class for retrieving OSGi service references. This class offers a
 * unified filter-based access for OSGi services as well as translation of
 * checked exceptions {@link InvalidSyntaxException} into unchecked ones.
 * 
 * <p/>
 * 
 * This classes uses {@link OsgiFilterUtils} underneath to allow multiple
 * classnames to be used for service reference lookup.
 * 
 * @see OsgiFilterUtils
 * @author Costin Leau
 * 
 */
public abstract class OsgiServiceReferenceUtils {

	public static ServiceReference getServiceReference(BundleContext bundleContext, String[] classes) {
		return getServiceReference(bundleContext, classes, null);
	}

	public static ServiceReference getServiceReference(BundleContext bundleContext, String clazz, String filter) {
		ServiceReference[] refs = getServiceReferences(bundleContext, clazz, filter);

		// pick one service
		ServiceReference winningReference = (refs.length > 0 ? refs[0] : null);
		
		if (winningReference == null)
			return null;
		
		long winningId = getServiceId(winningReference);
		int winningRanking = getServiceRanking(winningReference);

		for (int i = 1; i < refs.length; i++) {
			ServiceReference reference = refs[i];
			int serviceRanking = getServiceRanking(reference);
			long serviceId = getServiceId(reference);

			if ((serviceRanking > winningRanking) || (serviceRanking == winningRanking && winningId > serviceId)) {
				winningReference = reference;
				winningId = serviceId;
				winningRanking = serviceRanking;
			}
		}

		return winningReference;
	}

	public static ServiceReference getServiceReference(BundleContext bundleContext, String[] classes, String filter) {
		// use #getServiceReference(BundleContext, String, String) method to
		// speed the service lookup process by
		// giving one class as a hint to the OSGi implementation

		String clazz = (ObjectUtils.isEmpty(classes) ? null : classes[0]);

		return getServiceReference(bundleContext, clazz, OsgiFilterUtils.unifyFilter(classes, filter));
	}

	public static ServiceReference getServiceReference(BundleContext bundleContext, String filter) {
		return getServiceReference(bundleContext, (String) null, filter);
	}

	public static ServiceReference[] getServiceReferences(BundleContext bundleContext, String[] classes) {
		return getServiceReferences(bundleContext, classes, null);
	}

	public static ServiceReference[] getServiceReferences(BundleContext bundleContext, String clazz, String filter) {
		Assert.notNull(bundleContext, "bundleContext should be not null");

		try {
			ServiceReference[] refs = bundleContext.getServiceReferences(clazz, filter);
			return (refs == null ? new ServiceReference[0] : refs);
		}
		catch (InvalidSyntaxException ise) {
			throw (RuntimeException) new IllegalArgumentException("invalid filter: " + ise.getFilter()).initCause(ise);
		}

	}

	public static ServiceReference[] getServiceReferences(BundleContext bundleContext, String[] classes, String filter) {
		// use #getServiceReferences(BundleContext, String, String) method to
		// speed the service lookup process by
		// giving one class as a hint to the OSGi implementation

		String clazz = (ObjectUtils.isEmpty(classes) ? null : classes[0]);
		return getServiceReferences(bundleContext, clazz, OsgiFilterUtils.unifyFilter(classes, filter));
	}

	/**
	 * Return the service reference that match the given filter. This method
	 * never return null - if no service is found an empty array will be
	 * returned.
	 * 
	 * @param bundleContext
	 * @param filter
	 * @return an array of service references that match the given filter.
	 */
	public static ServiceReference[] getServiceReferences(BundleContext bundleContext, String filter) {
		return getServiceReferences(bundleContext, (String) null, filter);
	}

	public static long getServiceId(ServiceReference reference) {
		Assert.notNull(reference);
		return ((Long) reference.getProperty(Constants.SERVICE_ID)).longValue();
	}

	public static int getServiceRanking(ServiceReference reference) {
		Assert.notNull(reference);

		Object ranking = reference.getProperty(Constants.SERVICE_RANKING);
		// if the property is not supplied or of incorrect type, use a
		// default
		return ((ranking != null && ranking instanceof Integer) ? ((Integer) ranking).intValue() : 0);
	}
}
