
package org.springframework.osgi.blueprint.reflect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.ReferenceNameValue;
import org.osgi.service.blueprint.reflect.ReferenceValue;
import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.util.CollectionUtils;

public class MutableServiceExportComponentMetadata extends MutableComponentMetadata implements
		ServiceExportComponentMetadata {

	private int exportMode = ServiceExportComponentMetadata.EXPORT_MODE_DISABLED;
	private Value exportedComponent;
	private String[] interfaceNames;
	private int ranking;
	private RegistrationListenerMetadata[] listeners = new RegistrationListenerMetadata[0];
	private Properties serviceProperties = new Properties();


	public MutableServiceExportComponentMetadata(String name, Value exportedComponent, String[] interfaces) {
		super(name);
		if (exportedComponent == null) {
			throw new IllegalArgumentException("exportedComponent cannot be null");
		}
		this.exportedComponent = exportedComponent;
		if (interfaces == null || interfaces.length == 0) {
			throw new IllegalArgumentException("must specify at least one interface name");
		}
		this.interfaceNames = interfaces;
	}

	public int getAutoExportMode() {
		return this.exportMode;
	}

	public void setAutoExportMode(int exportMode) {
		if ((exportMode < EXPORT_MODE_DISABLED) || (exportMode > EXPORT_MODE_ALL)) {
			throw new IllegalArgumentException("unrecognised export mode: " + exportMode);
		}
		this.exportMode = exportMode;
	}

	public Value getExportedComponent() {
		return this.exportedComponent;
	}

	public void setExportedComponent(Value exportedComponent) {
		if (exportedComponent == null) {
			throw new IllegalArgumentException("exportedComponent cannot be null");
		}
		if (!isComponentBasedValue(exportedComponent)) {
			throw new IllegalArgumentException("value must refer to a component");
		}
		this.exportedComponent = exportedComponent;
	}

	private boolean isComponentBasedValue(Value exportedComponent) {
		return ((exportedComponent instanceof ComponentValue) || (exportedComponent instanceof ReferenceValue) || (exportedComponent instanceof ReferenceNameValue));
	}

	public Set<String> getInterfaceNames() {
		Set<String> intfs = new LinkedHashSet<String>(interfaceNames.length);
		CollectionUtils.mergeArrayIntoCollection(interfaceNames, intfs);
		return Collections.unmodifiableSet(intfs);
	}

	public void setInterfaceNames(String[] interfaceNames) {
		if ((interfaceNames == null) || (interfaceNames.length == 0)) {
			throw new IllegalArgumentException("must be at least one interface name");
		}
		this.interfaceNames = interfaceNames;
	}

	public int getRanking() {
		return this.ranking;
	}

	public void setRanking(int ranking) {
		this.ranking = ranking;
	}

	public Collection getRegistrationListeners() {
		return Collections.unmodifiableList(Arrays.asList(listeners));
	}

	public void setRegistrationListenerMetadata(RegistrationListenerMetadata[] listeners) {
		if (listeners == null) {
			this.listeners = new RegistrationListenerMetadata[0];
		}
		for (RegistrationListenerMetadata rlm : listeners) {
			if (rlm == null) {
				throw new IllegalArgumentException("registration listener cannot be null");
			}
		}
		this.listeners = listeners;
	}

	public Properties getServiceProperties() {
		return this.serviceProperties;
	}

	public void setServiceProperties(Properties serviceProperties) {
		if (serviceProperties == null) {
			this.serviceProperties = new Properties();
		}
		else {
			this.serviceProperties = serviceProperties;
		}
	}
}