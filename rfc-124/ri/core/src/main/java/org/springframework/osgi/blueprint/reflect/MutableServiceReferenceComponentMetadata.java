
package org.springframework.osgi.blueprint.reflect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.springframework.util.CollectionUtils;

public abstract class MutableServiceReferenceComponentMetadata extends MutableComponentMetadata implements
		ServiceReferenceComponentMetadata {

	private BindingListenerMetadata[] listeners = new BindingListenerMetadata[0];
	private String filter = "";
	private String[] interfaces = new String[0];
	private int serviceAvailability = ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY;


	public MutableServiceReferenceComponentMetadata(String name) {
		super(name);
	}

	public Collection<BindingListenerMetadata> getBindingListeners() {
		return Collections.unmodifiableList(Arrays.asList(listeners));
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

	public Set<String> getInterfaceNames() {
		Set<String> intfs = new LinkedHashSet<String>(interfaces.length);
		CollectionUtils.mergeArrayIntoCollection(interfaces, intfs);
		return Collections.unmodifiableSet(intfs);
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
		if ((serviceAvailability != ServiceReferenceComponentMetadata.MANDATORY_AVAILABILITY)
				&& (serviceAvailability != ServiceReferenceComponentMetadata.OPTIONAL_AVAILABILITY)) {
			throw new IllegalArgumentException("unknown availability value: " + serviceAvailability);
		}
		this.serviceAvailability = serviceAvailability;
	}
}