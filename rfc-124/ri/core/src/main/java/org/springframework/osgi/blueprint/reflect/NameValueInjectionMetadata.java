package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.Value;

public abstract class NameValueInjectionMetadata {

	private final String name;
	private Value value;

	public NameValueInjectionMetadata(String name, Value v) {
		if (null == name) {
			throw new IllegalArgumentException("name cannot be null");
		}
		if (null == v) {
			throw new IllegalArgumentException("value cannot be null");
		}
		this.name = name;
		this.value = v;
	}

	public String getName() {
		return this.name;
	}

	public Value getValue() {
		return this.value;
	}

	public void setValue(Value v) {
		if (null == v) {
			throw new IllegalArgumentException("value cannot be null");
		}
		this.value = v;
	}

}