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
package org.springframework.osgi.service.support.cardinality;

import org.osgi.framework.ServiceReference;
import org.springframework.osgi.service.NoSuchServiceException;
import org.springframework.osgi.service.support.DefaultRetryCallback;
import org.springframework.osgi.service.support.ServiceWrapper;

/**
 * TargetSource suitable for 0..1 and 1..1 cardinality.
 * 
 * @author Costin Leau
 * 
 */
public class SingleServiceCardinality extends OsgiServiceTargetSource {

	private ServiceWrapper wrapper;

	private boolean mandatoryEnd = true;

	public SingleServiceCardinality() {
		this(true);
	}

	public SingleServiceCardinality(boolean mandatoryEnd) {
		this.mandatoryEnd = mandatoryEnd;
	}

	protected ServiceWrapper lookupService() {

		return (ServiceWrapper) retryTemplate.execute(new DefaultRetryCallback() {
			public Object doWithRetry() {
				ServiceReference ref = context.getServiceReference(clazz);
				return (ref != null ? new ServiceWrapper(ref, context) : null);
			}
		});

	}

	public Object getTarget() throws Exception {
		// see if we have an alive target
		if (wrapper != null)
			synchronized (wrapper.getReference()) {
				if (wrapper.isServiceAlive())
					wrapper.getService();
			}

		// if not, look for one
		wrapper = lookupService();

		if (wrapper == null) {
			// handle the 1.. case
			if (mandatoryEnd) {
				throw new NoSuchServiceException("could not find service", null, null);
			}
			
			// TODO: this is wrong!
			return null;
		}
		return wrapper;
	}

}
