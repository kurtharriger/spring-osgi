package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.Value;

public class MutableRegistrationListenerMetadata implements
		RegistrationListenerMetadata {

	private final Value listenerComponent;
	private String registrationMethodName;
	private String unregistrationMethodName;
	
	public MutableRegistrationListenerMetadata(Value v, String reg, String unreg) {
		if (null == v) {
			throw new IllegalArgumentException("value cannot be null");
		}
		this.listenerComponent = v;
		this.registrationMethodName = reg;
		this.unregistrationMethodName = unreg;
	}
	
	public Value getListenerComponent() {
		return this.listenerComponent;
	}

	public String getRegistrationMethodName() {
		return this.registrationMethodName;
	}
	
	public void setRegistrationMethodName(String registrationMethodName) {
		this.registrationMethodName = registrationMethodName;
	}

	public String getUnregistrationMethodName() {
		return this.unregistrationMethodName;
	}

	public void setUnregistrationMethodName(String unregistrationMethodName) {
		this.unregistrationMethodName = unregistrationMethodName;
	}
}
