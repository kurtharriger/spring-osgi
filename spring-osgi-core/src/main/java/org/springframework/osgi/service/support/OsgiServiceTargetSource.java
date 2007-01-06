/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.osgi.service.support;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.aop.TargetSource;
import org.springframework.osgi.service.ServiceUnavailableException;
import org.springframework.util.Assert;

/**
 * OSGi service lookup target source.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiServiceTargetSource implements TargetSource {

	private static final Log log = LogFactory.getLog(OsgiServiceTargetSource.class);

	private BundleContext context;

	private final String filter;

	private int retryNumber;

	private long retryInterval;

	/** an array of services found inside the OSGi space **/
	private Object[] target;

	// map between services and their references
	private Map serviceToRefs = new HashMap();

	public OsgiServiceTargetSource(BundleContext context) {
		this(context, null);
	}

	public OsgiServiceTargetSource(BundleContext context, String filter) {
		Assert.notNull(context, "bundleContext required");
		this.context = context;
		this.filter = filter;
		// register a service listener
	}

	/**
	 * Check if a service is still alive.
	 * 
	 * @param target
	 * @return
	 */
	private boolean isTargetAlive(Object target) {
		ServiceReference ref = (ServiceReference) serviceToRefs.get(target);

		return (ref != null && context.getService(ref) != null);
	}

	public Object getTarget() throws Exception {
		if (isTargetAlive(target))
			return target;

		int count = 0;
		boolean noServicesFound = true;

		Object[] services;

		do {
			count++;
			//services = tracker.getServices();

			//noServicesFound = (ObjectUtils.isEmpty(services));

			if (noServicesFound) {
				//tracker.waitForService(retryInterval);
			}
			else {
				//target = services;
				return target;
			}

		} while (count < retryNumber);

		throw new ServiceUnavailableException("service could not be found", null, filter);
	}

	public Class getTargetClass() {
		return (target != null ? target.getClass() : null);
	}

	public boolean isStatic() {
		return false;
	}

	public void releaseTarget(Object target) {
		// TODO: add an ungetService here
		log.info("releasing target " + target);
	}

}
