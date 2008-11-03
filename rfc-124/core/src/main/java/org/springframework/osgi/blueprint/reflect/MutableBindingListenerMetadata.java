package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.Value;

public class MutableBindingListenerMetadata implements BindingListenerMetadata {

	private String bindMethod;
	private String unbindMethod;
	private final Value listenerComponent;
	
	public MutableBindingListenerMetadata(Value v, String bind, String unbind) {
		if (null == v) {
			throw new IllegalArgumentException("referenced listener cannot be null");
		}
		this.listenerComponent = v;
		this.bindMethod = bind;
		this.unbindMethod = unbind;
	}
	
	public String getBindMethodName() {
		return this.bindMethod;
	}
	
	public void setBindMethod(String bindMethod) {
		this.bindMethod = bindMethod;
	}

	public String getUnbindMethodName() {
		return this.unbindMethod;
	}
	
	public void setUnbindMethod(String unbindMethod) {
		this.unbindMethod = unbindMethod;
	}

	public Value getListenerComponent() {
		return this.listenerComponent;
	}

}
