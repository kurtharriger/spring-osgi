/*
 * Copyright 2008 the original author or authors.
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
package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;

public abstract class MutableServiceReferenceComponentMetadata extends
		MutableComponentMetadata implements ServiceReferenceComponentMetadata {

	private BindingListenerMetadata[] listeners = new BindingListenerMetadata[0];
	private String filter = "";
	private String[] interfaces = new String[0];
	private int serviceAvailability = ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY;

	public MutableServiceReferenceComponentMetadata(String name) {
		super(name);
	}
	
	public BindingListenerMetadata[] getBindingListeners() {
		return this.listeners;
	}
	
	public void setBindlingListeners(BindingListenerMetadata[] listeners) {
		if (listeners == null) {
			this.listeners = new BindingListenerMetadata[0];
		}
		else {
			for (BindingListenerMetadata l : listeners) {
				if (null == l) {
					throw new IllegalArgumentException("listener cannot be null");
				}
			}
			this.listeners = listeners;
		}
	}

	public String getFilter() {
		return this.filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}
	
	public String[] getInterfaceNames() {
		return this.interfaces;
	}
	
	public void setInterfaceNames(String[] interfaces) {
		if (null == interfaces) {
			this.interfaces = new String[0];
		}
		else {
			for (String i : interfaces) {
				if (null == i) {
					throw new IllegalArgumentException("interface name cannot be null");
				}
			}
			this.interfaces = interfaces;
		}
	}

	public int getServiceAvailabilitySpecification() {
		return this.serviceAvailability;
	}

	public void setServiceAvailability(int serviceAvailability) {
		if ((serviceAvailability != ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY) &&
			(serviceAvailability != ServiceReferenceComponentMetadata.OPTIONAL_AVAILABILITY)) {
			throw new IllegalArgumentException("unknown availability value: " + serviceAvailability);
		}
		this.serviceAvailability = serviceAvailability;
	}
}
