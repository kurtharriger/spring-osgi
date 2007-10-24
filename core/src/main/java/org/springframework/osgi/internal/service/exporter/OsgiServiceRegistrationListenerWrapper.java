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
package org.springframework.osgi.internal.service.exporter;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.osgi.internal.config.TargetSourceLifecycleListenerWrapper;
import org.springframework.osgi.service.OsgiServiceRegistrationListener;

/**
 * Adapter/wrapper class that handles listener with custom method invocation.
 * Similar in functionality to {@link TargetSourceLifecycleListenerWrapper}.
 * 
 * @author Costin Leau
 * 
 */
// FIXME: not yet implemented
public class OsgiServiceRegistrationListenerWrapper implements OsgiServiceRegistrationListener {

	private static final Log log = LogFactory.getLog(OsgiServiceRegistrationListenerWrapper.class);

	private final Object target;

	private final boolean isListener;

	private String registrationMethod, unregistrationMethod;

	public OsgiServiceRegistrationListenerWrapper(Object object) {
		this.target = object;
		isListener = target instanceof OsgiServiceRegistrationListener;
	}

	public void registered(Map serviceProperties) {
		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("invoking registered method with props=" + serviceProperties);

		// first call interface method (if it exists)
		if (isListener) {
			if (trace)
				log.trace("invoking listener interface methods");

			try {
				((OsgiServiceRegistrationListener) target).registered(serviceProperties);
			}
			catch (Exception ex) {
				log.warn("standard bind method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}
		else
			log.warn("custom registration listener methods not supported yet");
	}

	public void unregistered(Map serviceProperties) {

		boolean trace = log.isTraceEnabled();

		if (trace)
			log.trace("invoking unregistered method with props=" + serviceProperties);

		// first call interface method (if it exists)
		if (isListener) {
			if (trace)
				log.trace("invoking listener interface methods");

			try {
				((OsgiServiceRegistrationListener) target).unregistered(serviceProperties);
			}
			catch (Exception ex) {
				log.warn("standard bind method on [" + target.getClass().getName() + "] threw exception", ex);
			}
		}
		else
			log.warn("custom registration listener methods not supported yet");
	}

	/**
	 * @param registrationMethod The registrationMethod to set.
	 */
	public void setRegistrationMethod(String registrationMethod) {
		this.registrationMethod = registrationMethod;
	}

	/**
	 * @param unregistrationMethod The unregistrationMethod to set.
	 */
	public void setUnregistrationMethod(String unregistrationMethod) {
		this.unregistrationMethod = unregistrationMethod;
	}

}
